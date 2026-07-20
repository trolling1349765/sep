package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary of a fund-usage plan drawing on this funding, shown in the funding
 * detail screen. Populated once Đợt 3 (FundUsagePlan) lands; empty until then.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingPlanSummary {

    private String code;
    private BigDecimal amount;
    private String status;
    private LocalDate createdAt;
}
