package fpt.capstone.service.impl;

import fpt.capstone.dto.request.AllocationPlanRequest;
import fpt.capstone.dto.request.DistributionRequest;
import fpt.capstone.dto.response.AllocationPlanResponse;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.entity.*;
import fpt.capstone.repository.*;
import fpt.capstone.service.DistributionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionRecordServiceImpl implements DistributionRecordService {

    private final AllocationPlanRepository allocationPlanRepository;
    private final GoodsDistributionRepository goodsDistributionRepository;
    private final GoodsInventoryRepository inventoryRepository;
    private final BenificiaryRepository benificiaryRepository;

    @Override
    @Transactional
    public AllocationPlanResponse createAllocationPlan(AllocationPlanRequest request) {
        AllocationPlan plan = AllocationPlan.builder()
                .planCode(request.getPlanCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .plannedDate(request.getPlannedDate())
                .notes(request.getNotes())
                .createAt(LocalDate.now())
                .build();
        plan = allocationPlanRepository.save(plan);
        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public AllocationPlanResponse updateAllocationPlan(int id, AllocationPlanRequest request) {
        AllocationPlan plan = allocationPlanRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch phân bổ"));

        plan.setPlanCode(request.getPlanCode());
        plan.setTitle(request.getTitle());
        plan.setDescription(request.getDescription());
        plan.setStatus(request.getStatus());
        plan.setPlannedDate(request.getPlannedDate());
        plan.setNotes(request.getNotes());
        plan.setUpdateAt(LocalDate.now());

        plan = allocationPlanRepository.save(plan);
        return toPlanResponse(plan);
    }

    @Override
    public AllocationPlanResponse getAllocationPlanById(int id) {
        AllocationPlan plan = allocationPlanRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch phân bổ"));
        return toPlanResponse(plan);
    }

    @Override
    public List<AllocationPlanResponse> getAllAllocationPlans() {
        return allocationPlanRepository.findAll().stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AllocationPlanResponse> getAllocationPlansByStatus(String status) {
        return allocationPlanRepository.findByStatus(status).stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAllocationPlan(int id) {
        AllocationPlan plan = allocationPlanRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch phân bổ"));
        plan.setIsDelete(true);
        plan.setUpdateAt(LocalDate.now());
        allocationPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public DistributionResponse createDistribution(DistributionRequest request) {
        AllocationPlan plan = null;
        if (request.getAllocationPlanId() != null) {
            plan = allocationPlanRepository.findById(request.getAllocationPlanId())
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch phân bổ"));
        }

        GoodsInventory inventory = null;
        if (request.getGoodsInventoryId() != null) {
            inventory = inventoryRepository.findById(request.getGoodsInventoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy vật phẩm trong kho"));

            // Check and reserve stock
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < request.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không đủ");
            }
            inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
            inventoryRepository.save(inventory);
        }

        Benificiary benificiary = null;
        if (request.getBenificiaryId() != null) {
            benificiary = benificiaryRepository.findById(request.getBenificiaryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy đối tượng thụ hưởng"));
        }

        GoodsDistribution distribution = GoodsDistribution.builder()
                .allocationPlan(plan)
                .goodsInventory(inventory)
                .benificiary(benificiary)
                .quantity(request.getQuantity())
                .transferDate(request.getTransferDate() != null ? request.getTransferDate() : LocalDate.now())
                .confirmationStatus("PENDING")
                .notes(request.getNotes())
                .createAt(LocalDate.now())
                .build();
        distribution = goodsDistributionRepository.save(distribution);

        // Update inventory after distribution confirmed
        if (inventory != null) {
            inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());
            inventoryRepository.save(inventory);
        }

        return toDistResponse(distribution);
    }

    @Override
    @Transactional
    public DistributionResponse confirmDistribution(int id, String confirmationStatus) {
        GoodsDistribution distribution = goodsDistributionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi cấp phát"));

        distribution.setConfirmationStatus(confirmationStatus);
        distribution.setUpdateAt(LocalDate.now());
        distribution = goodsDistributionRepository.save(distribution);

        return toDistResponse(distribution);
    }

    @Override
    public DistributionResponse getDistributionById(int id) {
        GoodsDistribution distribution = goodsDistributionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi cấp phát"));
        return toDistResponse(distribution);
    }

    @Override
    public List<DistributionResponse> getDistributionsByPlan(int planId) {
        return goodsDistributionRepository.findByAllocationPlanId(planId).stream()
                .map(this::toDistResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistributionResponse> getAllDistributions() {
        return goodsDistributionRepository.findAll().stream()
                .map(this::toDistResponse)
                .collect(Collectors.toList());
    }

    private AllocationPlanResponse toPlanResponse(AllocationPlan plan) {
        return AllocationPlanResponse.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .status(plan.getStatus())
                .plannedDate(plan.getPlannedDate())
                .completedDate(plan.getCompletedDate())
                .notes(plan.getNotes())
                .createAt(plan.getCreateAt())
                .updateAt(plan.getUpdateAt())
                .build();
    }

    private DistributionResponse toDistResponse(GoodsDistribution dist) {
        return DistributionResponse.builder()
                .id(dist.getId())
                .allocationPlanId(dist.getAllocationPlan() != null ? dist.getAllocationPlan().getId() : null)
                .planCode(dist.getAllocationPlan() != null ? dist.getAllocationPlan().getPlanCode() : null)
                .goodsInventoryId(dist.getGoodsInventory() != null ? dist.getGoodsInventory().getId() : null)
                .itemName(dist.getGoodsInventory() != null ? dist.getGoodsInventory().getItemName() : null)
                .benificiaryId(dist.getBenificiary() != null ? dist.getBenificiary().getId() : null)
                .benificiaryName(dist.getBenificiary() != null ? dist.getBenificiary().getFullName() : null)
                .quantity(dist.getQuantity())
                .transferDate(dist.getTransferDate())
                .confirmationStatus(dist.getConfirmationStatus())
                .notes(dist.getNotes())
                .createAt(dist.getCreateAt())
                .build();
    }
}