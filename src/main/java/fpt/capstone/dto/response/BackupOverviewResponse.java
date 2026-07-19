package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The "Thiết lập và lần sao lưu gần nhất" block. latest is null when the
 * system has never run a backup — the FE renders its empty state from that.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupOverviewResponse {

    private ScheduleInfo schedule;
    private BackupResponse latest;

    // nextRunAt is null when the schedule is disabled (or the cron is invalid).
    public record ScheduleInfo(boolean enabled, String cron, LocalDateTime nextRunAt) {
    }
}
