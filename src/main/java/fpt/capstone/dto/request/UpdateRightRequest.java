package fpt.capstone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Only display fields are editable; code and isSystem are immutable (spec 9.5). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRightRequest {

    private String name;

    private String description;
}
