package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.DistributionCreateRequest;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Distribution;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.DistributionStatus;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.PlanLineStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.DistributionRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.ItemAllocationPlanLineRepository;
import fpt.capstone.repository.ItemAllocationPlanRepository;
import fpt.capstone.service.DistributionService;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DistributionServiceImpl implements DistributionService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "issueDate", "issueDate");

    private final DistributionRepository distributionRepository;
    private final ItemAllocationPlanLineRepository lineRepository;
    private final ItemAllocationPlanRepository planRepository;
    private final GoodsInventoryRepository goodsInventoryRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DistributionResponse> search(int page, int size, String itemId, Integer beneficiaryId,
                                                     String planId, LocalDate fromDate, LocalDate toDate,
                                                     DistributionStatus status, String sort, String dir) {
        if (page < 0 || size < 1) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        String property = SORT_WHITELIST.get(sort);
        if (property == null) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.DESC;
        } else {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));
        Page<Distribution> result = distributionRepository.search(
                itemId, beneficiaryId, planId, status, fromDate, toDate, pageable);
        return PageResponse.from(result.map(DistributionResponse::from));
    }

    @Override
    @Transactional
    @Auditable(action = Action.DISTRIBUTION_CREATE, entity = Table.DISTRIBUTION)
    public DistributionResponse create(DistributionCreateRequest request, List<MultipartFile> files) {
        ItemAllocationPlanLine line = lineRepository.lockById(request.getPlanLineId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.PLAN_LINE_NOT_FOUND.name()));
        ItemAllocationPlan plan = line.getPlan();
        if (plan.getStatus() != PlanStatus.APPROVED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        if (line.getLineStatus() != PlanLineStatus.PENDING) {
            throw badRequest(ErrorCode.LINE_ALREADY_ISSUED);
        }
        if (request.getActualQty() > line.getPlannedQty()) {
            throw badRequest(ErrorCode.QUANTITY_EXCEEDS_RESERVED);
        }
        if (files == null || files.isEmpty()) {
            throw badRequest(ErrorCode.EVIDENCE_REQUIRED);
        }

        // Distribution removes stock from BOTH on-hand and reserved (available unchanged).
        GoodsInventory inv = goodsInventoryRepository.lockByItemId(plan.getItem().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
        inv.setQuantityOnHand(inv.getQuantityOnHand() - request.getActualQty());
        inv.setReservedQuantity(inv.getReservedQuantity() - request.getActualQty());
        inv.setUpdateAt(LocalDate.now());
        goodsInventoryRepository.save(inv);

        line.setIssuedQty(line.getIssuedQty() + request.getActualQty());
        line.setLineStatus(PlanLineStatus.ISSUED);
        lineRepository.save(line);

        String actor = securityUtil.getCurrentUserId();
        Distribution distribution = Distribution.builder()
                .code(codeGenerator.next("CP"))
                .planLine(line)
                .beneficiary(line.getBeneficiary())
                .recipientName(request.getRecipientName())
                .recipientRelationship(request.getRecipientRelationship())
                .actualQty(request.getActualQty())
                .issueDate(request.getIssueDate())
                .issuingOfficer(actor)
                .note(request.getNote())
                .status(DistributionStatus.ISSUED)
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        distribution = distributionRepository.save(distribution);
        resourceAttachmentService.saveAll(Table.DISTRIBUTION, distribution.getId(),
                AttachmentKind.SIGNED_RECEIPT, files);

        recomputeCompletion(plan);
        return DistributionResponse.from(distribution);
    }

    @Override
    @Transactional
    @Auditable(action = Action.DISTRIBUTION_CONFIRM, entity = Table.DISTRIBUTION)
    public DistributionResponse confirm(String id) {
        Distribution distribution = distributionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.DISTRIBUTION_NOT_FOUND.name()));
        if (distribution.getStatus() != DistributionStatus.ISSUED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        distribution.setStatus(DistributionStatus.RECIPIENT_CONFIRMED);
        distribution.setConfirmedAt(LocalDateTime.now());
        distribution = distributionRepository.save(distribution);
        return DistributionResponse.from(distribution);
    }

    private void recomputeCompletion(ItemAllocationPlan plan) {
        if (plan.getStatus() == PlanStatus.APPROVED
                && lineRepository.countByPlanIdAndLineStatus(plan.getId(), PlanLineStatus.PENDING) == 0) {
            plan.setStatus(PlanStatus.COMPLETED);
            planRepository.save(plan);
        }
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
