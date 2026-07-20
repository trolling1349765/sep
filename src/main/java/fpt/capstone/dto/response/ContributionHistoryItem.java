package fpt.capstone.dto.response;

import fpt.capstone.enums.ContributionKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line of a sponsor's contribution history — a union of funding records
 * (kind=FUNDING, with amount) and inbound receipts (kind=ITEM, amount null).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionHistoryItem {

    private ContributionKind kind;
    private String code;
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String status;
}
