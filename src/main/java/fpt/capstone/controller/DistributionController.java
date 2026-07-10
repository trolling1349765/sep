package fpt.capstone.controller;

import fpt.capstone.dto.request.AllocationPlanRequest;
import fpt.capstone.dto.request.DistributionRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AllocationPlanResponse;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.service.DistributionRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionRecordService distributionService;

    // === Allocation Plan endpoints ===

    @PostMapping("/plans")
    public ResponseEntity<APIResponse<AllocationPlanResponse>> createPlan(
            @Valid @RequestBody AllocationPlanRequest request) {
        AllocationPlanResponse response = distributionService.createAllocationPlan(request);
        return ResponseEntity.ok(APIResponse.success("Tạo kế hoạch phân bổ vật phẩm thành công", response));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<APIResponse<AllocationPlanResponse>> updatePlan(
            @PathVariable int id,
            @Valid @RequestBody AllocationPlanRequest request) {
        AllocationPlanResponse response = distributionService.updateAllocationPlan(id, request);
        return ResponseEntity.ok(APIResponse.success("Cập nhật kế hoạch phân bổ thành công", response));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<APIResponse<AllocationPlanResponse>> getPlanById(@PathVariable int id) {
        AllocationPlanResponse response = distributionService.getAllocationPlanById(id);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping("/plans")
    public ResponseEntity<APIResponse<List<AllocationPlanResponse>>> getAllPlans(
            @RequestParam(required = false) String status) {
        List<AllocationPlanResponse> responses;
        if (status != null) {
            responses = distributionService.getAllocationPlansByStatus(status);
        } else {
            responses = distributionService.getAllAllocationPlans();
        }
        return ResponseEntity.ok(APIResponse.success(responses));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<APIResponse<Void>> deletePlan(@PathVariable int id) {
        distributionService.deleteAllocationPlan(id);
        return ResponseEntity.ok(APIResponse.success("Xóa kế hoạch phân bổ thành công", null));
    }

    // === Distribution endpoints ===

    @PostMapping("/distribute")
    public ResponseEntity<APIResponse<DistributionResponse>> distributeItems(
            @Valid @RequestBody DistributionRequest request) {
        DistributionResponse response = distributionService.createDistribution(request);
        return ResponseEntity.ok(APIResponse.success("Cấp phát vật phẩm thành công", response));
    }

    @PatchMapping("/distribute/{id}/confirm")
    public ResponseEntity<APIResponse<DistributionResponse>> confirmDistribution(
            @PathVariable int id,
            @RequestParam String status) {
        DistributionResponse response = distributionService.confirmDistribution(id, status);
        return ResponseEntity.ok(APIResponse.success("Cập nhật trạng thái cấp phát thành công", response));
    }

    @GetMapping("/distribute/{id}")
    public ResponseEntity<APIResponse<DistributionResponse>> getDistributionById(@PathVariable int id) {
        DistributionResponse response = distributionService.getDistributionById(id);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping("/distribute")
    public ResponseEntity<APIResponse<List<DistributionResponse>>> getAllDistributions(
            @RequestParam(required = false) Integer planId) {
        List<DistributionResponse> responses;
        if (planId != null) {
            responses = distributionService.getDistributionsByPlan(planId);
        } else {
            responses = distributionService.getAllDistributions();
        }
        return ResponseEntity.ok(APIResponse.success(responses));
    }
}