package fpt.capstone.dto.response;

import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.enums.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Row of the item-plan list (GET /item-plans). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPlanListResponse {

    private String id;
    private String code;
    private String itemId;
    private String itemName;
    private int plannedQty;
    private PlanStatus status;
    private LocalDate expectedDate;
    private String deliveryPlace;
    private LocalDate createdAt;
    private String createdBy;

    public static ItemPlanListResponse from(ItemAllocationPlan p) {
        return ItemPlanListResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .itemId(p.getItem() != null ? p.getItem().getId() : null)
                .itemName(p.getItem() != null ? p.getItem().getName() : null)
                .plannedQty(p.getPlannedQty())
                .status(p.getStatus())
                .expectedDate(p.getExpectedDate())
                .deliveryPlace(p.getDeliveryPlace())
                .createdAt(p.getCreateAt())
                .createdBy(p.getCreateBy())
                .build();
    }
}
