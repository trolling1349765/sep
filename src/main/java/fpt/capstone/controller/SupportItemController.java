package fpt.capstone.controller;

import fpt.capstone.dto.request.SupportItemRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SupportItemResponse;
import fpt.capstone.service.SupportItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Support-item catalog (Vat pham ho tro) — UC 5.7.8, with quick-create from the receipt form. */
@RestController
@RequestMapping("/support-items")
@RequiredArgsConstructor
public class SupportItemController {

    private final SupportItemService supportItemService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPORT_ITEM_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<SupportItemResponse>>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                supportItemService.search(page, size, q, sort, dir)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPORT_ITEM_RECEIVE')")
    public ResponseEntity<APIResponse<SupportItemResponse>> createItem(
            @Valid @RequestBody SupportItemRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Tạo vật phẩm mới thành công.", supportItemService.quickCreate(request)));
    }
}
