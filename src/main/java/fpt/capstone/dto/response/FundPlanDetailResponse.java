package fpt.capstone.dto.response;

import fpt.capstone.entity.FundUsagePlan;
import fpt.capstone.enums.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fund-plan detail (GET /fund-plans/{id}), including a DELETED plan (with its reason).
 * {@code getId()} feeds the audit entityId. Attachments are split by kind; the
 * timeline is reconstructed from the lifecycle fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundPlanDetailResponse {

    private String id;
    private String code;
    private Integer donationId;
    private String donationCode;
    private Integer beneficiaryId;
    private String beneficiaryName;
    private String programName;
    private BigDecimal amount;
    private String purpose;
    private LocalDate expectedDate;
    private String deliveryPlace;
    private PlanStatus status;
    private String createdBy;
    private String approvedBy;
    private String rejectReason;
    private String cancelReason;
    private String deleteReason;
    private List<AttachmentResponse> listAttachments;
    private List<AttachmentResponse> completionAttachments;
    private List<PlanTimelineEntry> timeline;

    public static FundPlanDetailResponse from(FundUsagePlan p,
                                              List<AttachmentResponse> listAttachments,
                                              List<AttachmentResponse> completionAttachments,
                                              List<PlanTimelineEntry> timeline) {
        return FundPlanDetailResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .donationId(p.getDonation() != null ? p.getDonation().getId() : null)
                .donationCode(p.getDonation() != null ? p.getDonation().getCode() : null)
                .beneficiaryId(p.getBeneficiary() != null ? p.getBeneficiary().getId() : null)
                .beneficiaryName(p.getBeneficiary() != null ? p.getBeneficiary().getFullName() : null)
                .programName(p.getProgramName())
                .amount(p.getAmount())
                .purpose(p.getPurpose())
                .expectedDate(p.getExpectedDate())
                .deliveryPlace(p.getDeliveryPlace())
                .status(p.getStatus())
                .createdBy(p.getCreateBy())
                .approvedBy(p.getApprovedBy())
                .rejectReason(p.getRejectReason())
                .cancelReason(p.getCancelReason())
                .deleteReason(p.getDeleteReason())
                .listAttachments(listAttachments)
                .completionAttachments(completionAttachments)
                .timeline(timeline)
                .build();
    }
}
