package fpt.capstone.controller;

import fpt.capstone.dto.request.StockAdjustmentRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.InventoryBalanceResponse;
import fpt.capstone.dto.response.InventoryTransactionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Stock balances + ledger + manual adjustments (Ton kho / Dieu chinh) — UC 5.7.10/5.7.11. */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<InventoryBalanceResponse>>> getBalances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                inventoryService.listBalances(page, size, sort, dir)));
    }

    @GetMapping("/{itemId}/transactions")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<InventoryTransactionResponse>>> getTransactions(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(APIResponse.success(
                inventoryService.transactions(itemId, page, size)));
    }

    @PostMapping(value = "/{itemId}/adjustments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public ResponseEntity<APIResponse<InventoryTransactionResponse>> adjust(
            @PathVariable String itemId,
            @Valid @RequestPart("data") StockAdjustmentRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật tồn kho thành công.", inventoryService.adjust(itemId, data, files)));
    }
}
