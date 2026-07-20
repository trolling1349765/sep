package fpt.capstone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of POST /item-plans/lines/{lineId}/not-delivered (marks a line as absent). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotDeliveredRequest {

    private String note;

    /** true → release the line's reserved stock back to available; false → keep it held. */
    private boolean returnStock;
}
