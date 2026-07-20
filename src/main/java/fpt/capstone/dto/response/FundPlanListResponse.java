package fpt.capstone.dto.response;

import fpt.capstone.entity.FundUsagePlan;
import fpt.capstone.enums.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of the fund-plan list (GET /fund-plans). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundPlanListResponse {

    private String id;
    private String code;
    private Integer donationId;
    private String donationCode;
    private String target;
    private BigDecimal amount;
    private PlanStatus status;
    private LocalDate expectedDate;
    private LocalDate createdAt;
    private String createdBy;

    public static FundPlanListResponse from(FundUsagePlan p) {
        String target = p.getBeneficiary() != null ? p.getBeneficiary().getFullName() : p.getProgramName();
        return FundPlanListResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .donationId(p.getDonation() != null ? p.getDonation().getId() : null)
                .donationCode(p.getDonation() != null ? p.getDonation().getCode() : null)
                .target(target)
                .amount(p.getAmount())
                .status(p.getStatus())
                .expectedDate(p.getExpectedDate())
                .createdAt(p.getCreateAt())
                .createdBy(p.getCreateBy())
                .build();
    }
}
