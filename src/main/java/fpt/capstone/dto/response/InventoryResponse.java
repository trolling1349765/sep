package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResponse {
    int id;
    String sponsorId;
    String sponsorName;
    String itemName;
    String unit;
    int quantity;
    int reservedQuantity;
    int availableQuantity;
    String conditionStatus;
    String location;
    LocalDate receiptDate;
    String status;
    String notes;
    LocalDate createAt;
}