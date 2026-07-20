package fpt.capstone.dto.response;

import fpt.capstone.entity.GoodsInventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Row of the stock-balance list (GET /inventory). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBalanceResponse {

    private String itemId;
    private String itemCode;
    private String itemName;
    private String unit;
    private int quantityOnHand;
    private int reservedQuantity;
    private int available;
    private LocalDate updatedAt;

    public static InventoryBalanceResponse from(GoodsInventory g) {
        return InventoryBalanceResponse.builder()
                .itemId(g.getItem() != null ? g.getItem().getId() : null)
                .itemCode(g.getItem() != null ? g.getItem().getCode() : null)
                .itemName(g.getItem() != null ? g.getItem().getName() : null)
                .unit(g.getItem() != null ? g.getItem().getUnit() : null)
                .quantityOnHand(g.getQuantityOnHand())
                .reservedQuantity(g.getReservedQuantity())
                .available(g.getQuantityOnHand() - g.getReservedQuantity())
                .updatedAt(g.getUpdateAt())
                .build();
    }
}
