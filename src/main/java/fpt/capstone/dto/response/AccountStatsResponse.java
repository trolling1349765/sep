package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatsResponse {
    private long total;
    private long active;
    private long pendingVerification;
    private long locked;
    private long banned;
    private long tempLocked;
}
