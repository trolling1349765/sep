package fpt.capstone.service.impl;

import fpt.capstone.dto.response.BackupOverviewResponse;
import fpt.capstone.dto.response.BackupResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Backup;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.BackupType;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.BackupRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private BackupRepository backupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemLogService systemLogService;
    @Mock
    private BackupFileStore fileStore;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private BackupServiceImpl backupService;

    private static final String YEAR_PREFIX = "BK-" + Year.now().getValue() + "-";

    private void stubSavePassthrough() {
        when(backupRepository.save(any(Backup.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubEmptyDump() {
        when(jdbcTemplate.queryForList(anyString())).thenAnswer(inv -> new ArrayList<Map<String, Object>>());
    }

    private void stubFileWrite() {
        when(fileStore.write(anyString(), any(BackupType.class), anyString(),
                any(LocalDateTime.class), anyMap()))
                .thenReturn(new BackupFileStore.WrittenFile(Path.of("backups/x.json"), 1234L, "abc123"));
    }

    private List<String> loggedActions() {
        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService, atLeastOnce()).write(captor.capture());
        return captor.getAllValues().stream().map(SystemLog::getAction).toList();
    }

    @Nested
    class Create {

        @Test
        void create_nullType_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.create((BackupType) null, "u1"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void create_invalidTypeString_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.create("INCREMENTAL"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void create_whileAnotherBackupRunning_throwsConflict() {
            AtomicBoolean lock = (AtomicBoolean) ReflectionTestUtils.getField(backupService, "backupRunning");
            lock.set(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.create(BackupType.FULL, "u1"));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals(ErrorCode.BACKUP_IN_PROGRESS.name(), ex.getReason());
            verifyNoInteractions(backupRepository);
        }

        @Test
        void create_firstBackupOfYear_getsSequence001() {
            stubSavePassthrough();
            stubEmptyDump();
            stubFileWrite();
            when(backupRepository.findMaxSequenceForPrefix(YEAR_PREFIX + "%")).thenReturn(null);

            BackupResponse response = backupService.create(BackupType.BUSINESS, "u1");

            assertEquals(YEAR_PREFIX + "001", response.getCode());
        }

        @Test
        void create_sequenceContinuesFromMax() {
            stubSavePassthrough();
            stubEmptyDump();
            stubFileWrite();
            when(backupRepository.findMaxSequenceForPrefix(YEAR_PREFIX + "%")).thenReturn(7);

            BackupResponse response = backupService.create(BackupType.BUSINESS, "u1");

            assertEquals(YEAR_PREFIX + "008", response.getCode());
        }

        @Test
        void create_success_completesRecordAndLogsCreateAndComplete() {
            stubSavePassthrough();
            stubFileWrite();
            when(backupRepository.findMaxSequenceForPrefix(anyString())).thenReturn(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", 1);
            row.put("created", java.sql.Timestamp.valueOf("2026-07-19 08:00:00"));
            when(jdbcTemplate.queryForList(anyString()))
                    .thenAnswer(inv -> new ArrayList<>(List.of(new LinkedHashMap<>(row))));
            when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "Admin A")));

            BackupResponse response = backupService.create(BackupType.BUSINESS, "u1");

            assertEquals(BackupStatus.COMPLETED.name(), response.getStatus());
            assertEquals(1234L, response.getSizeBytes());
            assertEquals("abc123", response.getChecksumSha256());
            assertEquals(11, response.getTableCount());     // BUSINESS scope size
            assertEquals(11L, response.getRowCount());      // one row per table
            assertEquals("u1", response.getCreatedBy());
            assertEquals("Admin A", response.getCreatedByName());
            assertNotNull(response.getCompletedAt());
            assertEquals(List.of("BACKUP_CREATE", "BACKUP_COMPLETE"), loggedActions());

            // Timestamp values must be normalized before serialization.
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, List<Map<String, Object>>>> dataCaptor =
                    ArgumentCaptor.forClass(Map.class);
            verify(fileStore).write(anyString(), eq(BackupType.BUSINESS), anyString(),
                    any(LocalDateTime.class), dataCaptor.capture());
            Object normalized = dataCaptor.getValue().get("applications").get(0).get("created");
            assertEquals("2026-07-19 08:00:00.000000", normalized);
        }

        @Test
        void create_dumpFails_savesFailedRecordLogsAndThrows500() {
            stubSavePassthrough();
            when(backupRepository.findMaxSequenceForPrefix(anyString())).thenReturn(null);
            when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("db gone"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.create(BackupType.FULL, "u1"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            ArgumentCaptor<Backup> saved = ArgumentCaptor.forClass(Backup.class);
            verify(backupRepository, times(2)).save(saved.capture());
            Backup last = saved.getAllValues().get(1);
            assertEquals(BackupStatus.FAILED, last.getStatus());
            assertEquals("db gone", last.getErrorMessage());
            assertNotNull(last.getCompletedAt());
            assertEquals(List.of("BACKUP_CREATE", "BACKUP_FAILED"), loggedActions());
        }

        @Test
        void create_lockReleasedAfterFailure_nextBackupRuns() {
            stubSavePassthrough();
            when(backupRepository.findMaxSequenceForPrefix(anyString())).thenReturn(null);
            when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("boom"));

            assertThrows(ResponseStatusException.class,
                    () -> backupService.create(BackupType.FULL, "u1"));

            // Second attempt must reach the repository again instead of 409ing.
            ResponseStatusException second = assertThrows(ResponseStatusException.class,
                    () -> backupService.create(BackupType.FULL, "u1"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, second.getStatusCode());
        }

        @Test
        void create_viaStringType_resolvesCurrentUser() {
            stubSavePassthrough();
            stubEmptyDump();
            stubFileWrite();
            when(backupRepository.findMaxSequenceForPrefix(anyString())).thenReturn(null);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-9");

            BackupResponse response = backupService.create("full");

            assertEquals("admin-9", response.getCreatedBy());
        }
    }

    @Nested
    class ListBackups {

        @Test
        void list_negativePage_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.list(-1, 20, null, null));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void list_invalidStatus_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.list(0, 20, "DONE", null));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void list_invalidType_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> backupService.list(0, 20, null, "PARTIAL"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void list_sizeClampedAndFiltersParsed() {
            when(backupRepository.search(eq(BackupStatus.COMPLETED), eq(BackupType.FULL),
                    any(Pageable.class))).thenReturn(Page.empty());

            backupService.list(0, 1000, "completed", "FULL");

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(backupRepository).search(eq(BackupStatus.COMPLETED), eq(BackupType.FULL),
                    pageableCaptor.capture());
            assertEquals(100, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void list_resolvesCreatorNamesInOneBatch_nullCreatorStaysNull() {
            Backup manual = backup(1, "BK-2026-001", "u1");
            Backup scheduled = backup(2, "BK-2026-002", null);
            when(backupRepository.search(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(manual, scheduled), PageRequest.of(0, 20), 2));
            when(userRepository.findAllById(List.of("u1")))
                    .thenReturn(List.of(user("u1", "Admin A")));

            PageResponse<BackupResponse> page = backupService.list(0, 20, null, null);

            assertEquals(2, page.getTotalElements());
            assertEquals("Admin A", page.getItems().get(0).getCreatedByName());
            assertNull(page.getItems().get(1).getCreatedByName());
            verify(userRepository, times(1)).findAllById(List.of("u1"));
        }
    }

    @Nested
    class Overview {

        @Test
        void overview_neverBackedUp_latestIsNull() {
            when(backupRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.empty());

            BackupOverviewResponse overview = backupService.overview();

            assertNull(overview.getLatest());
            assertNotNull(overview.getSchedule());
        }

        @Test
        void overview_scheduleDisabled_nextRunAtNull() {
            ReflectionTestUtils.setField(backupService, "scheduledEnabled", false);
            ReflectionTestUtils.setField(backupService, "cron", "0 0 2 * * MON");
            when(backupRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.empty());

            BackupOverviewResponse overview = backupService.overview();

            assertFalse(overview.getSchedule().enabled());
            assertEquals("0 0 2 * * MON", overview.getSchedule().cron());
            assertNull(overview.getSchedule().nextRunAt());
        }

        @Test
        void overview_scheduleEnabled_computesNextRunFromCron() {
            ReflectionTestUtils.setField(backupService, "scheduledEnabled", true);
            ReflectionTestUtils.setField(backupService, "cron", "0 0 2 * * MON");
            when(backupRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.empty());

            BackupOverviewResponse overview = backupService.overview();

            assertNotNull(overview.getSchedule().nextRunAt());
            assertTrue(overview.getSchedule().nextRunAt().isAfter(LocalDateTime.now()));
        }

        @Test
        void overview_invalidCron_nextRunAtNull() {
            ReflectionTestUtils.setField(backupService, "scheduledEnabled", true);
            ReflectionTestUtils.setField(backupService, "cron", "not-a-cron");
            when(backupRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.empty());

            BackupOverviewResponse overview = backupService.overview();

            assertNull(overview.getSchedule().nextRunAt());
        }

        @Test
        void overview_latestPresent_mappedWithCreatorName() {
            Backup latest = backup(5, "BK-2026-005", "u1");
            when(backupRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.of(latest));
            when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "Admin A")));

            BackupOverviewResponse overview = backupService.overview();

            assertEquals("BK-2026-005", overview.getLatest().getCode());
            assertEquals("Admin A", overview.getLatest().getCreatedByName());
        }
    }

    private static Backup backup(int id, String code, String createdBy) {
        Backup b = Backup.builder()
                .code(code)
                .type(BackupType.FULL)
                .status(BackupStatus.COMPLETED)
                .createdBy(createdBy)
                .startedAt(LocalDateTime.now())
                .build();
        b.setId(id);
        return b;
    }

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }
}
