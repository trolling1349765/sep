package fpt.capstone.controller;

import fpt.capstone.dto.request.ItemPlanRequest;
import fpt.capstone.dto.request.NotDeliveredRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ItemPlanDetailResponse;
import fpt.capstone.dto.response.ItemPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.service.ItemAllocationPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Item allocation plans (Ke hoach phan bo vat pham) — UC 5.7.12. Header + n lines,
 * approve reserves stock, cancel/soft-delete/reject branches, per-line not-delivered.
 * Creator (ITEM_ALLOCATION_PLAN_CREATE) is never the approver (ITEM_PLAN_APPROVE).
 */
@RestController
@RequestMapping("/item-plans")
@RequiredArgsConstructor
public class ItemAllocationPlanController {

    private final ItemAllocationPlanService itemAllocationPlanService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<ItemPlanListResponse>>> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                itemAllocationPlanService.search(page, size, q, itemId, status, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(itemAllocationPlanService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ITEM_ALLOCATION_PLAN_CREATE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> createPlan(
            @Valid @RequestPart("data") ItemPlanRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Tạo kế hoạch phân bổ vật phẩm thành công.", itemAllocationPlanService.create(data, files)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ITEM_ALLOCATION_PLAN_CREATE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> updatePlan(
            @PathVariable String id,
            @Valid @RequestPart("data") ItemPlanRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật kế hoạch thành công.", itemAllocationPlanService.update(id, data, files)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ITEM_PLAN_APPROVE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> approvePlan(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(
                "Phê duyệt kế hoạch thành công.", itemAllocationPlanService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ITEM_PLAN_APPROVE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> rejectPlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đã từ chối kế hoạch.", itemAllocationPlanService.reject(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ITEM_ALLOCATION_PLAN_CREATE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> cancelPlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Hủy kế hoạch — đã hoàn trả giữ chỗ.", itemAllocationPlanService.cancel(id, request)));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('ITEM_ALLOCATION_PLAN_CREATE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> deletePlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đã xóa kế hoạch.", itemAllocationPlanService.delete(id, request)));
    }

    @PostMapping("/lines/{lineId}/not-delivered")
    @PreAuthorize("hasAuthority('ITEM_DISTRIBUTE')")
    public ResponseEntity<APIResponse<ItemPlanDetailResponse>> markNotDelivered(
            @PathVariable String lineId, @RequestBody NotDeliveredRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đã đánh dấu vắng mặt.", itemAllocationPlanService.markNotDelivered(lineId, request)));
    }
}
