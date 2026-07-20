package fpt.capstone.dto.response;

import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.enums.PlanLineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One line of an item allocation plan in the detail view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPlanLineResponse {

    private String id;
    private Integer beneficiaryId;
    private String beneficiaryName;
    private int plannedQty;
    private int issuedQty;
    private PlanLineStatus lineStatus;
    private String notDeliveredNote;

    public static ItemPlanLineResponse from(ItemAllocationPlanLine l) {
        return ItemPlanLineResponse.builder()
                .id(l.getId())
                .beneficiaryId(l.getBeneficiary() != null ? l.getBeneficiary().getId() : null)
                .beneficiaryName(l.getBeneficiary() != null ? l.getBeneficiary().getFullName() : null)
                .plannedQty(l.getPlannedQty())
                .issuedQty(l.getIssuedQty())
                .lineStatus(l.getLineStatus())
                .notDeliveredNote(l.getNotDeliveredNote())
                .build();
    }
}
