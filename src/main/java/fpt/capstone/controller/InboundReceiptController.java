package fpt.capstone.controller;

import fpt.capstone.dto.request.InboundReceiptRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.InboundReceiptDetailResponse;
import fpt.capstone.dto.response.InboundReceiptListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.ReceiptStatus;
import fpt.capstone.service.InboundReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Inbound goods receipts (Phieu nhap kho) — UC 5.7.7/5.7.9. Create = tiep nhan (DRAFT,
 * stock untouched); post = ghi so (adds on-hand, BR-52). DRAFT is editable; POSTED is locked.
 */
@RestController
@RequestMapping("/inbound-receipts")
@RequiredArgsConstructor
public class InboundReceiptController {

    private final InboundReceiptService inboundReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<InboundReceiptListResponse>>> getReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ReceiptStatus status,
            @RequestParam(required = false) String itemId,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                inboundReceiptService.search(page, size, q, status, itemId, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<InboundReceiptDetailResponse>> getReceipt(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(inboundReceiptService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SUPPORT_ITEM_RECEIVE') and hasAuthority('INBOUND_RECEIPT_CREATE')")
    public ResponseEntity<APIResponse<InboundReceiptDetailResponse>> createReceipt(
            @Valid @RequestPart("data") InboundReceiptRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Tiếp nhận vật phẩm hỗ trợ thành công.", inboundReceiptService.create(data, files)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INBOUND_RECEIPT_CREATE')")
    public ResponseEntity<APIResponse<InboundReceiptDetailResponse>> updateReceipt(
            @PathVariable String id,
            @Valid @RequestPart("data") InboundReceiptRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật phiếu nhập thành công.", inboundReceiptService.update(id, data, files)));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('INBOUND_RECEIPT_POST')")
    public ResponseEntity<APIResponse<InboundReceiptDetailResponse>> postReceipt(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(
                "Lập phiếu nhập kho thành công.", inboundReceiptService.post(id)));
    }
}
