package fpt.capstone.dto.response;

import fpt.capstone.entity.SupportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Support item row with joined stock balance (GET /support-items). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportItemResponse {

    private String id;
    private String code;
    private String name;
    private String unit;
    private String description;
    private int quantityOnHand;
    private int reservedQuantity;
    private int available;

    public static SupportItemResponse from(SupportItem item, int onHand, int reserved) {
        return SupportItemResponse.builder()
                .id(item.getId())
                .code(item.getCode())
                .name(item.getName())
                .unit(item.getUnit())
                .description(item.getDescription())
                .quantityOnHand(onHand)
                .reservedQuantity(reserved)
                .available(onHand - reserved)
                .build();
    }
}
