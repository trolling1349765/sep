package fpt.capstone.scheduler;

import fpt.capstone.enums.BackupType;
import fpt.capstone.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled FULL backup (default: 02:00 every Monday). Shares
 * BackupService.create with the manual endpoint — same concurrency lock, same
 * audit logging. createdBy = null marks the run as "Hệ thống".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupSchedulerJob {

    private final BackupService backupService;

    @Value("${app.backup.scheduled-enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * MON}")
    public void weeklyBackup() {
        if (!enabled) {
            return;
        }
        try {
            backupService.create(BackupType.FULL, null);
        } catch (Exception e) {
            // Never rethrow: the FAILED record + BACKUP_FAILED audit row are
            // already written by the service; the scheduler must stay alive.
            log.error("Scheduled backup failed", e);
        }
    }
}
