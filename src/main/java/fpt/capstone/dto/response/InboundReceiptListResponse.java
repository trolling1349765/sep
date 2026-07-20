package fpt.capstone.dto.response;

import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.enums.ItemCondition;
import fpt.capstone.enums.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Row of the inbound-receipt list (GET /inbound-receipts). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundReceiptListResponse {

    private String id;
    private String code;
    private String itemName;
    private String sponsorName;
    private int quantity;
    private ItemCondition condition;
    private LocalDate receiveDate;
    private ReceiptStatus status;

    public static InboundReceiptListResponse from(InboundReceipt r) {
        return InboundReceiptListResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .itemName(r.getItem() != null ? r.getItem().getName() : null)
                .sponsorName(r.getSponsor() != null ? r.getSponsor().getName() : null)
                .quantity(r.getQuantity())
                .condition(r.getCondition())
                .receiveDate(r.getReceiveDate())
                .status(r.getStatus())
                .build();
    }
}
