package fpt.capstone.dto.response;

import fpt.capstone.entity.Distribution;
import fpt.capstone.enums.DistributionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Distribution record (GET /distributions, POST /distributions). {@code getId()} feeds audit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionResponse {

    private String id;
    private String code;
    private String planId;
    private String planCode;
    private String planLineId;
    private Integer beneficiaryId;
    private String beneficiaryName;
    private String itemName;
    private String recipientName;
    private String recipientRelationship;
    private int actualQty;
    private LocalDate issueDate;
    private String issuingOfficer;
    private String note;
    private DistributionStatus status;
    private LocalDateTime confirmedAt;

    public static DistributionResponse from(Distribution d) {
        var line = d.getPlanLine();
        var plan = line != null ? line.getPlan() : null;
        return DistributionResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .planId(plan != null ? plan.getId() : null)
                .planCode(plan != null ? plan.getCode() : null)
                .planLineId(line != null ? line.getId() : null)
                .beneficiaryId(d.getBeneficiary() != null ? d.getBeneficiary().getId() : null)
                .beneficiaryName(d.getBeneficiary() != null ? d.getBeneficiary().getFullName() : null)
                .itemName(plan != null && plan.getItem() != null ? plan.getItem().getName() : null)
                .recipientName(d.getRecipientName())
                .recipientRelationship(d.getRecipientRelationship())
                .actualQty(d.getActualQty())
                .issueDate(d.getIssueDate())
                .issuingOfficer(d.getIssuingOfficer())
                .note(d.getNote())
                .status(d.getStatus())
                .confirmedAt(d.getConfirmedAt())
                .build();
    }
}
