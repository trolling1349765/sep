package fpt.capstone.service;

import fpt.capstone.dto.request.AllocationPlanRequest;
import fpt.capstone.dto.request.DistributionRequest;
import fpt.capstone.dto.response.AllocationPlanResponse;
import fpt.capstone.dto.response.DistributionResponse;

import java.util.List;

public interface DistributionRecordService {
    AllocationPlanResponse createAllocationPlan(AllocationPlanRequest request);

    AllocationPlanResponse updateAllocationPlan(int id, AllocationPlanRequest request);

    AllocationPlanResponse getAllocationPlanById(int id);

    List<AllocationPlanResponse> getAllAllocationPlans();

    List<AllocationPlanResponse> getAllocationPlansByStatus(String status);

    void deleteAllocationPlan(int id);

    DistributionResponse createDistribution(DistributionRequest request);

    DistributionResponse confirmDistribution(int id, String confirmationStatus);

    DistributionResponse getDistributionById(int id);

    List<DistributionResponse> getDistributionsByPlan(int planId);

    List<DistributionResponse> getAllDistributions();
}