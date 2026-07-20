package fpt.capstone.dto.response;

import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.enums.ItemCondition;
import fpt.capstone.enums.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Inbound-receipt detail (GET /inbound-receipts/{id}). {@code getId()} feeds the audit entityId. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundReceiptDetailResponse {

    private String id;
    private String code;
    private String itemId;
    private String itemName;
    private String itemUnit;
    private String sponsorId;
    private String sponsorName;
    private String delivererName;
    private int quantity;
    private ItemCondition condition;
    private LocalDate receiveDate;
    private String evidenceName;
    private ReceiptStatus status;
    private String postedBy;
    private LocalDateTime postedAt;
    private List<AttachmentResponse> attachments;

    public static InboundReceiptDetailResponse from(InboundReceipt r, List<AttachmentResponse> attachments) {
        return InboundReceiptDetailResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .itemId(r.getItem() != null ? r.getItem().getId() : null)
                .itemName(r.getItem() != null ? r.getItem().getName() : null)
                .itemUnit(r.getItem() != null ? r.getItem().getUnit() : null)
                .sponsorId(r.getSponsor() != null ? r.getSponsor().getId() : null)
                .sponsorName(r.getSponsor() != null ? r.getSponsor().getName() : null)
                .delivererName(r.getDelivererName())
                .quantity(r.getQuantity())
                .condition(r.getCondition())
                .receiveDate(r.getReceiveDate())
                .evidenceName(r.getEvidenceName())
                .status(r.getStatus())
                .postedBy(r.getPostedBy())
                .postedAt(r.getPostedAt())
                .attachments(attachments)
                .build();
    }
}
