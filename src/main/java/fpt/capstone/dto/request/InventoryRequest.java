package fpt.capstone.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryRequest {
    String sponsorId;

    @NotBlank(message = "Tên vật phẩm không được để trống")
    String itemName;

    String unit;

    @Min(value = 0, message = "Số lượng không được âm")
    int quantity;

    String conditionStatus;

    String location;

    LocalDate receiptDate;

    String notes;
}