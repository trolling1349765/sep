package fpt.capstone.controller;

import fpt.capstone.dto.request.InventoryRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.InventoryResponse;
import fpt.capstone.service.GoodsInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final GoodsInventoryService inventoryService;

    @PostMapping
    public ResponseEntity<APIResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.ok(APIResponse.success("Tiếp nhận vật phẩm hỗ trợ thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<InventoryResponse>> updateInventory(
            @PathVariable int id,
            @Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(APIResponse.success("Cập nhật vật phẩm thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<InventoryResponse>> getInventoryById(@PathVariable int id) {
        InventoryResponse response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<InventoryResponse>>> getAllInventories(
            @RequestParam(required = false) String keyword) {
        List<InventoryResponse> responses;
        if (keyword != null) {
            responses = inventoryService.searchByItemName(keyword);
        } else {
            responses = inventoryService.getAllInventories();
        }
        return ResponseEntity.ok(APIResponse.success(responses));
    }

    @PatchMapping("/{id}/adjust")
    public ResponseEntity<APIResponse<InventoryResponse>> adjustQuantity(
            @PathVariable int id,
            @RequestParam int quantity,
            @RequestParam String reason) {
        InventoryResponse response = inventoryService.adjustQuantity(id, quantity, reason);
        return ResponseEntity.ok(APIResponse.success("Cập nhật tồn kho thành công", response));
    }

    @PostMapping("/{id}/inbound")
    public ResponseEntity<APIResponse<InventoryResponse>> createInboundReceipt(
            @PathVariable int id,
            @Valid @RequestBody InventoryRequest request) {
        // Create inventory item as inbound receipt
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.ok(APIResponse.success("Lập phiếu nhập kho thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteInventory(@PathVariable int id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.ok(APIResponse.success("Xóa vật phẩm thành công", null));
    }
}