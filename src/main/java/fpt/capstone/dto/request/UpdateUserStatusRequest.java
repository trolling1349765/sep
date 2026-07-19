package fpt.capstone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of PUT /admin/users/{id}/status. `status` is a String, not the enum:
 * only ACTIVE/INACTIVE are legal here and anything else (including BANNED,
 * which has no admin flow yet) must map to ErrorCode.INVALID_STATUS instead
 * of a generic Jackson parse error.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    private String status;
}
