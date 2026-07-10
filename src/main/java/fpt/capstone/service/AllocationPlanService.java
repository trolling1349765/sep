package fpt.capstone.service;

import fpt.capstone.entity.AllocationPlan;
import fpt.capstone.entity.GoodsDistribution;

import java.util.List;

public interface AllocationPlanService {
    AllocationPlan createPlan(AllocationPlan plan);

    AllocationPlan updatePlan(AllocationPlan plan);

    AllocationPlan getPlanById(int id);

    List<AllocationPlan> getAllPlans();

    List<AllocationPlan> getPlansByStatus(String status);

    void deletePlan(int id);

    List<GoodsDistribution> getDistributionsByPlan(int planId);
}