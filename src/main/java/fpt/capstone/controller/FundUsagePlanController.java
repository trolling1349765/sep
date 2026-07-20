package fpt.capstone.controller;

import fpt.capstone.dto.request.FundPlanRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.FundPlanDetailResponse;
import fpt.capstone.dto.response.FundPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.service.FundUsagePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Fund-usage plan (Ke hoach su dung kinh phi) — UC 5.7.6. Create → approve (reserve
 * pending) → complete (spend) → done; reject/cancel/soft-delete branches. Creator
 * (FUNDING_ALLOCATE) is never the approver (FUNDING_PLAN_APPROVE). Every write audits.
 */
@RestController
@RequestMapping("/fund-plans")
@RequiredArgsConstructor
public class FundUsagePlanController {

    private final FundUsagePlanService fundUsagePlanService;

    @GetMapping
    @PreAuthorize("hasAuthority('FUNDING_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<FundPlanListResponse>>> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer donationId,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) Integer beneficiaryId,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                fundUsagePlanService.search(page, size, q, donationId, status, beneficiaryId, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FUNDING_VIEW')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> getPlan(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(fundUsagePlanService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FUNDING_ALLOCATE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> createPlan(
            @Valid @RequestPart("data") FundPlanRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Gửi kế hoạch chờ duyệt thành công.", fundUsagePlanService.create(data, files)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FUNDING_ALLOCATE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> updatePlan(
            @PathVariable String id,
            @Valid @RequestPart("data") FundPlanRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật kế hoạch thành công.", fundUsagePlanService.update(id, data, files)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FUNDING_PLAN_APPROVE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> approvePlan(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(
                "Phê duyệt kế hoạch thành công.", fundUsagePlanService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('FUNDING_PLAN_APPROVE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> rejectPlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đã từ chối kế hoạch.", fundUsagePlanService.reject(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FUNDING_ALLOCATE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> cancelPlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Hủy kế hoạch — đã hoàn trả nguồn.", fundUsagePlanService.cancel(id, request)));
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FUNDING_ALLOCATE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> completePlan(
            @PathVariable String id,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Xác nhận hoàn thành kế hoạch thành công.", fundUsagePlanService.complete(id, files)));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('FUNDING_ALLOCATE')")
    public ResponseEntity<APIResponse<FundPlanDetailResponse>> deletePlan(
            @PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đã xóa kế hoạch.", fundUsagePlanService.delete(id, request)));
    }
}
