package fpt.capstone.dto.response;

import fpt.capstone.entity.Donation;
import fpt.capstone.enums.FundingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of the funding list (GET /fundings). Balance columns are computed at runtime. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingListResponse {

    private int id;
    private String code;
    private String name;
    private String sponsorName;
    private BigDecimal amount;
    private BigDecimal availableAmount;
    private BigDecimal pendingAmount;
    private BigDecimal spentAmount;
    private LocalDate receivedDate;
    private FundingStatus status;

    public static FundingListResponse from(Donation d) {
        BigDecimal pending = d.getPendingAmount() != null ? d.getPendingAmount() : BigDecimal.ZERO;
        BigDecimal spent = d.getSpentAmount() != null ? d.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal amount = d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO;
        return FundingListResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .sponsorName(d.getSponsor() != null ? d.getSponsor().getName() : null)
                .amount(amount)
                .availableAmount(amount.subtract(pending).subtract(spent))
                .pendingAmount(pending)
                .spentAmount(spent)
                .receivedDate(d.getTransferDate())
                .status(d.getStatus())
                .build();
    }
}
