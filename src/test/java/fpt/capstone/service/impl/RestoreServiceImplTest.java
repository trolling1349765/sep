package fpt.capstone.service.impl;

import fpt.capstone.dto.request.RestoreRequest;
import fpt.capstone.dto.response.RestoreResultResponse;
import fpt.capstone.entity.Backup;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.BackupType;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.BackupRepository;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreServiceImplTest {

    @Mock
    private BackupRepository backupRepository;
    @Mock
    private BackupFileStore fileStore;
    @Mock
    private RestoreTxExecutor restoreTxExecutor;
    @Mock
    private SystemLogService systemLogService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private RestoreServiceImpl restoreService;

    @TempDir
    Path tempDir;

    private Path backupFile;
    private Backup completed;

    @BeforeEach
    void setUp() throws IOException {
        backupFile = tempDir.resolve("BK-2026-001.json");
        Files.writeString(backupFile, "{}");
        completed = Backup.builder()
                .code("BK-2026-001")
                .type(BackupType.FULL)
                .status(BackupStatus.COMPLETED)
                .filePath(backupFile.toString())
                .checksumSha256("goodsum")
                .build();
        completed.setId(1);
    }

    private static RestoreRequest confirmed(Integer backupId) {
        return RestoreRequest.builder().backupId(backupId).confirm(true).build();
    }

    private Map<String, List<Map<String, Object>>> dataOf(String table) {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        data.put(table, List.of(Map.of("id", 1)));
        return data;
    }

    private List<String> loggedActions() {
        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService, atLeastOnce()).write(captor.capture());
        return captor.getAllValues().stream().map(SystemLog::getAction).toList();
    }

    @Nested
    class Guards {

        @Test
        void restore_nullRequest_throwsConfirmRequired() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(null));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.CONFIRM_REQUIRED.name(), ex.getReason());
            verifyNoInteractions(backupRepository, restoreTxExecutor);
        }

        @Test
        void restore_confirmFalse_throwsConfirmRequired() {
            RestoreRequest request = RestoreRequest.builder().backupId(1).confirm(false).build();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.CONFIRM_REQUIRED.name(), ex.getReason());
            verifyNoInteractions(backupRepository, restoreTxExecutor);
        }

        @Test
        void restore_nullBackupId_throwsNotFound() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void restore_unknownBackup_throwsNotFound() {
            when(backupRepository.findById(99)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(99)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void restore_everyNonCompletedStatus_throwsNotRestorable() {
            for (BackupStatus status : List.of(BackupStatus.RUNNING, BackupStatus.FAILED,
                    BackupStatus.CORRUPTED)) {
                completed.setStatus(status);
                when(backupRepository.findById(1)).thenReturn(Optional.of(completed));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                        () -> restoreService.restore(confirmed(1)), status.name());
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertEquals(ErrorCode.BACKUP_NOT_RESTORABLE.name(), ex.getReason());
            }
            verifyNoInteractions(restoreTxExecutor);
        }

        @Test
        void restore_missingFile_marksCorruptedAndThrowsConflict() {
            completed.setFilePath(tempDir.resolve("gone.json").toString());
            when(backupRepository.findById(1)).thenReturn(Optional.of(completed));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(1)));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_CORRUPTED.name(), ex.getReason());
            assertEquals(BackupStatus.CORRUPTED, completed.getStatus());
            verify(backupRepository).save(completed);
            verifyNoInteractions(restoreTxExecutor);
        }

        @Test
        void restore_checksumMismatch_marksCorruptedAndThrowsConflict() {
            when(backupRepository.findById(1)).thenReturn(Optional.of(completed));
            when(fileStore.sha256(backupFile)).thenReturn("tampered");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(1)));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_CORRUPTED.name(), ex.getReason());
            assertEquals(BackupStatus.CORRUPTED, completed.getStatus());
            verify(backupRepository).save(completed);
            verifyNoInteractions(restoreTxExecutor);
        }

        @Test
        void restore_tableOutsideScope_failsBeforeAnyDbWork() {
            when(backupRepository.findById(1)).thenReturn(Optional.of(completed));
            when(fileStore.sha256(backupFile)).thenReturn("goodsum");
            when(fileStore.read(backupFile))
                    .thenReturn(new BackupFileStore.ParsedBackup(Map.of(), dataOf("users")));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(1)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_NOT_RESTORABLE.name(), ex.getReason());
            assertEquals(List.of("RESTORE_FAILED"), loggedActions());
            verifyNoInteractions(restoreTxExecutor);
        }
    }

    @Nested
    class Execution {

        @Test
        void restore_success_logsStartBeforeExecutorAndCompleteAfter() {
            when(backupRepository.findById(1)).thenReturn(Optional.of(completed));
            when(fileStore.sha256(backupFile)).thenReturn("GOODSUM"); // case-insensitive match
            when(fileStore.read(backupFile))
                    .thenReturn(new BackupFileStore.ParsedBackup(Map.of(), dataOf("applications")));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(restoreTxExecutor.execute(anyMap()))
                    .thenReturn(new RestoreTxExecutor.RestoreStats(1, 42));

            RestoreResultResponse response = restoreService.restore(confirmed(1));

            assertEquals("BK-2026-001", response.getBackupCode());
            assertEquals(1, response.getRestoredTables());
            assertEquals(42, response.getRestoredRows());
            assertTrue(response.getDurationMs() >= 0);
            assertEquals(List.of("RESTORE_START", "RESTORE_COMPLETE"), loggedActions());

            InOrder inOrder = inOrder(systemLogService, restoreTxExecutor);
            inOrder.verify(systemLogService).write(argThat(l -> "RESTORE_START".equals(l.getAction())));
            inOrder.verify(restoreTxExecutor).execute(anyMap());
            inOrder.verify(systemLogService).write(argThat(l -> "RESTORE_COMPLETE".equals(l.getAction())));

            ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
            verify(systemLogService, times(2)).write(logCaptor.capture());
            assertEquals("admin-1", logCaptor.getAllValues().get(0).getUserId());
            assertEquals("BK-2026-001", logCaptor.getAllValues().get(0).getEntityId());
            assertEquals("BACKUPS", logCaptor.getAllValues().get(0).getEntityType());
        }

        @Test
        void restore_executorFails_logsRestoreFailedAndThrows500() {
            when(backupRepository.findById(1)).thenReturn(Optional.of(completed));
            when(fileStore.sha256(backupFile)).thenReturn("goodsum");
            when(fileStore.read(backupFile))
                    .thenReturn(new BackupFileStore.ParsedBackup(Map.of(), dataOf("applications")));
            when(restoreTxExecutor.execute(anyMap())).thenThrow(new RuntimeException("fk violation"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> restoreService.restore(confirmed(1)));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertEquals(List.of("RESTORE_START", "RESTORE_FAILED"), loggedActions());
        }
    }
}
