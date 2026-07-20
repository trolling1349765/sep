package fpt.capstone.dto.response;

import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.enums.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Item-plan detail (GET /item-plans/{id}). {@code getId()} feeds the audit entityId. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPlanDetailResponse {

    private String id;
    private String code;
    private String itemId;
    private String itemName;
    private String itemUnit;
    private int plannedQty;
    private LocalDate expectedDate;
    private String deliveryPlace;
    private String deliveryTimeWindow;
    private PlanStatus status;
    private String createdBy;
    private String approvedBy;
    private String rejectReason;
    private String cancelReason;
    private String deleteReason;
    private List<ItemPlanLineResponse> lines;
    private List<AttachmentResponse> listAttachments;
    private List<PlanTimelineEntry> timeline;

    public static ItemPlanDetailResponse from(ItemAllocationPlan p, List<ItemPlanLineResponse> lines,
                                              List<AttachmentResponse> listAttachments,
                                              List<PlanTimelineEntry> timeline) {
        return ItemPlanDetailResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .itemId(p.getItem() != null ? p.getItem().getId() : null)
                .itemName(p.getItem() != null ? p.getItem().getName() : null)
                .itemUnit(p.getItem() != null ? p.getItem().getUnit() : null)
                .plannedQty(p.getPlannedQty())
                .expectedDate(p.getExpectedDate())
                .deliveryPlace(p.getDeliveryPlace())
                .deliveryTimeWindow(p.getDeliveryTimeWindow())
                .status(p.getStatus())
                .createdBy(p.getCreateBy())
                .approvedBy(p.getApprovedBy())
                .rejectReason(p.getRejectReason())
                .cancelReason(p.getCancelReason())
                .deleteReason(p.getDeleteReason())
                .lines(lines)
                .listAttachments(listAttachments)
                .timeline(timeline)
                .build();
    }
}
