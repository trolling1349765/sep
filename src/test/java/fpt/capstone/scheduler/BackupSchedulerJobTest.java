package fpt.capstone.scheduler;

import fpt.capstone.enums.BackupType;
import fpt.capstone.service.BackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerJobTest {

    @Mock
    private BackupService backupService;

    @InjectMocks
    private BackupSchedulerJob job;

    @Test
    void weeklyBackup_disabled_doesNothing() {
        ReflectionTestUtils.setField(job, "enabled", false);

        job.weeklyBackup();

        verifyNoInteractions(backupService);
    }

    @Test
    void weeklyBackup_enabled_runsFullBackupAsSystem() {
        ReflectionTestUtils.setField(job, "enabled", true);

        job.weeklyBackup();

        verify(backupService).create(eq(BackupType.FULL), isNull());
    }

    @Test
    void weeklyBackup_serviceThrows_neverPropagates() {
        ReflectionTestUtils.setField(job, "enabled", true);
        when(backupService.create(eq(BackupType.FULL), isNull()))
                .thenThrow(new RuntimeException("backup blew up"));

        assertDoesNotThrow(job::weeklyBackup);
    }
}
