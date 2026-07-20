package fpt.capstone.dto.response;

import fpt.capstone.entity.Donation;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Funding detail (GET /fundings/{id}). {@code getId()} feeds the audit entityId.
 * The three balance figures are computed at runtime, not stored as availability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingDetailResponse {

    private int id;
    private String code;
    private String name;
    private String sponsorId;
    private String sponsorName;
    private BigDecimal amount;
    private BigDecimal availableAmount;
    private BigDecimal pendingAmount;
    private BigDecimal spentAmount;
    private String purpose;
    private PaymentMethod paymentMethod;
    private String transactionRef;
    private String evidenceName;
    private LocalDate receivedDate;
    private FundingStatus status;
    private String recordedBy;
    private List<AttachmentResponse> attachments;
    private List<FundingPlanSummary> plans;

    public static FundingDetailResponse from(Donation d, List<AttachmentResponse> attachments,
                                             List<FundingPlanSummary> plans) {
        BigDecimal pending = d.getPendingAmount() != null ? d.getPendingAmount() : BigDecimal.ZERO;
        BigDecimal spent = d.getSpentAmount() != null ? d.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal amount = d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO;
        return FundingDetailResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .sponsorId(d.getSponsor() != null ? d.getSponsor().getId() : null)
                .sponsorName(d.getSponsor() != null ? d.getSponsor().getName() : null)
                .amount(amount)
                .availableAmount(amount.subtract(pending).subtract(spent))
                .pendingAmount(pending)
                .spentAmount(spent)
                .purpose(d.getPurpose())
                .paymentMethod(d.getPaymentMethod())
                .transactionRef(d.getTransactionRef())
                .evidenceName(d.getEvidenceName())
                .receivedDate(d.getTransferDate())
                .status(d.getStatus())
                .recordedBy(d.getRecordedBy())
                .attachments(attachments)
                .plans(plans)
                .build();
    }
}
