package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One entry of an item's merged inventory ledger (GET /inventory/{itemId}/transactions):
 * INBOUND (+), ADJUSTMENT (±, with balanceAfter), or DISTRIBUTION (−, added in Đợt 5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {

    private String id;
    private String type;
    private String refCode;
    private int delta;
    private String reason;
    private Integer balanceAfter;
    private LocalDateTime occurredAt;
    private String performedBy;
}
