package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private AccountStatsResponse accounts;
    private List<RecentActivityResponse> recentActivities;
    private OperationalStatusResponse operational;
}
