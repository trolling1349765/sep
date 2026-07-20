package fpt.capstone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of PUT /sponsors/{id}/status. `status` is a String, not the enum: only
 * ACTIVE/INACTIVE are legal and anything else must map to ErrorCode.INVALID_STATUS
 * rather than a generic Jackson parse error.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSponsorStatusRequest {

    private String status;
}
