package fpt.capstone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequest {
    private Integer backupId;
    // Must be literal true — the FE confirmation checkbox is enforced server-side.
    private Boolean confirm;
}
