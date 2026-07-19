package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResultResponse {
    private String backupCode;
    private int restoredTables;
    private long restoredRows;
    private long durationMs;
}
