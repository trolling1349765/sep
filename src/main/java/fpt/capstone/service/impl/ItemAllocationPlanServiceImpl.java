package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.ItemPlanLineRequest;
import fpt.capstone.dto.request.ItemPlanRequest;
import fpt.capstone.dto.request.NotDeliveredRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.dto.response.ItemPlanDetailResponse;
import fpt.capstone.dto.response.ItemPlanLineResponse;
import fpt.capstone.dto.response.ItemPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.PlanTimelineEntry;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.entity.User;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.NotificationType;
import fpt.capstone.enums.PlanLineStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.ItemAllocationPlanLineRepository;
import fpt.capstone.repository.ItemAllocationPlanRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.ItemAllocationPlanService;
import fpt.capstone.service.NotificationService;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemAllocationPlanServiceImpl implements ItemAllocationPlanService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "expectedDate", "expectedDate");

    private final ItemAllocationPlanRepository planRepository;
    private final ItemAllocationPlanLineRepository lineRepository;
    private final SupportItemRepository supportItemRepository;
    private final BenificiaryRepository benificiaryRepository;
    private final GoodsInventoryRepository goodsInventoryRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final NotificationService notificationService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemPlanListResponse> search(int page, int size, String q, String itemId,
                                                     PlanStatus status, String sort, String dir) {
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
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));
        Page<ItemAllocationPlan> result = planRepository.search(query, itemId, status, pageable);
        return PageResponse.from(result.map(ItemPlanListResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemPlanDetailResponse getById(String id) {
        return toDetail(requirePlan(id));
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_CREATE, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse create(ItemPlanRequest request, List<MultipartFile> files) {
        validateLineSum(request);
        SupportItem item = requireItem(request.getItemId());
        requireWithinAvailable(item.getId(), request.getPlannedQty());
        requireFiles(files);

        String actor = securityUtil.getCurrentUserId();
        ItemAllocationPlan plan = ItemAllocationPlan.builder()
                .code(codeGenerator.next("KHPP"))
                .item(item)
                .plannedQty(request.getPlannedQty())
                .expectedDate(request.getExpectedDate())
                .deliveryPlace(request.getDeliveryPlace())
                .deliveryTimeWindow(request.getDeliveryTimeWindow())
                .status(PlanStatus.PENDING_APPROVAL)
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        plan = planRepository.save(plan);
        saveLines(plan, request.getLines());
        resourceAttachmentService.saveAll(Table.ITEM_ALLOCATION_PLAN, plan.getId(), AttachmentKind.LIST, files);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_UPDATE, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse update(String id, ItemPlanRequest request, List<MultipartFile> files) {
        ItemAllocationPlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.REJECTED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        validateLineSum(request);
        SupportItem item = requireItem(request.getItemId());
        requireWithinAvailable(item.getId(), request.getPlannedQty());

        plan.setItem(item);
        plan.setPlannedQty(request.getPlannedQty());
        plan.setExpectedDate(request.getExpectedDate());
        plan.setDeliveryPlace(request.getDeliveryPlace());
        plan.setDeliveryTimeWindow(request.getDeliveryTimeWindow());
        if (plan.getStatus() == PlanStatus.REJECTED) {
            plan.setStatus(PlanStatus.PENDING_APPROVAL);
            plan.setRejectReason(null);
        }
        plan.setUpdateAt(LocalDate.now());
        plan.setUpdateBy(securityUtil.getCurrentUserId());
        planRepository.save(plan);
        // Replace the whole line set (only allowed while not yet approved).
        lineRepository.deleteAll(lineRepository.findByPlanId(plan.getId()));
        saveLines(plan, request.getLines());
        if (files != null && !files.isEmpty()) {
            resourceAttachmentService.saveAll(Table.ITEM_ALLOCATION_PLAN, plan.getId(), AttachmentKind.LIST, files);
        }
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_APPROVE, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse approve(String id) {
        ItemAllocationPlan plan = requirePlan(id);
        String actor = securityUtil.getCurrentUserId();
        if (actor != null && actor.equals(plan.getCreateBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.SELF_APPROVAL_FORBIDDEN.name());
        }
        requireStatus(plan, PlanStatus.PENDING_APPROVAL);
        GoodsInventory inv = lockInventory(plan.getItem().getId());
        if (plan.getPlannedQty() > available(inv)) {
            throw badRequest(ErrorCode.STOCK_INSUFFICIENT);
        }
        inv.setReservedQuantity(inv.getReservedQuantity() + plan.getPlannedQty());
        inv.setUpdateAt(LocalDate.now());
        goodsInventoryRepository.save(inv);

        plan.setStatus(PlanStatus.APPROVED);
        plan.setApprovedBy(actor);
        plan.setApprovedAt(LocalDateTime.now());
        planRepository.save(plan);

        scheduleNotifications(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_REJECT, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse reject(String id, ReasonRequest request) {
        ItemAllocationPlan plan = requirePlan(id);
        requireStatus(plan, PlanStatus.PENDING_APPROVAL);
        String reason = requireReason(request);
        plan.setStatus(PlanStatus.REJECTED);
        plan.setRejectReason(reason);
        planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_CANCEL, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse cancel(String id, ReasonRequest request) {
        ItemAllocationPlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.APPROVED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        String reason = requireReason(request);
        if (plan.getStatus() == PlanStatus.APPROVED) {
            List<ItemAllocationPlanLine> lines = lineRepository.findByPlanId(plan.getId());
            int release = lines.stream().mapToInt(l -> l.getPlannedQty() - l.getIssuedQty()).sum();
            GoodsInventory inv = lockInventory(plan.getItem().getId());
            inv.setReservedQuantity(inv.getReservedQuantity() - release);
            inv.setUpdateAt(LocalDate.now());
            goodsInventoryRepository.save(inv);
            for (ItemAllocationPlanLine line : lines) {
                if (line.getLineStatus() == PlanLineStatus.PENDING) {
                    line.setLineStatus(PlanLineStatus.NOT_DELIVERED);
                    lineRepository.save(line);
                }
            }
        }
        plan.setStatus(PlanStatus.CANCELLED);
        plan.setCancelReason(reason);
        planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.ITEM_PLAN_DELETE, entity = Table.ITEM_ALLOCATION_PLAN)
    public ItemPlanDetailResponse delete(String id, ReasonRequest request) {
        ItemAllocationPlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.REJECTED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        String reason = requireReason(request);
        plan.setStatus(PlanStatus.DELETED);
        plan.setDeleteReason(reason);
        plan.setDeletedBy(securityUtil.getCurrentUserId());
        plan.setDeletedAt(LocalDateTime.now());
        planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.DISTRIBUTION_NOT_DELIVERED, entity = Table.ITEM_ALLOCATION_PLAN_LINE)
    public ItemPlanDetailResponse markNotDelivered(String lineId, NotDeliveredRequest request) {
        ItemAllocationPlanLine line = lineRepository.lockById(lineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.PLAN_LINE_NOT_FOUND.name()));
        ItemAllocationPlan plan = line.getPlan();
        if (plan.getStatus() != PlanStatus.APPROVED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        if (line.getLineStatus() != PlanLineStatus.PENDING) {
            throw badRequest(ErrorCode.LINE_ALREADY_ISSUED);
        }
        if (request != null && request.isReturnStock()) {
            int held = line.getPlannedQty() - line.getIssuedQty();
            GoodsInventory inv = lockInventory(plan.getItem().getId());
            inv.setReservedQuantity(inv.getReservedQuantity() - held);
            inv.setUpdateAt(LocalDate.now());
            goodsInventoryRepository.save(inv);
            line.setIssuedQty(line.getPlannedQty());
        }
        line.setLineStatus(PlanLineStatus.NOT_DELIVERED);
        line.setNotDeliveredNote(request != null ? request.getNote() : null);
        lineRepository.save(line);
        recomputeCompletion(plan);
        return toDetail(plan);
    }

    // ---- guards & helpers ----

    private ItemAllocationPlan requirePlan(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.PLAN_NOT_FOUND.name()));
    }

    private void requireStatus(ItemAllocationPlan plan, PlanStatus expected) {
        if (plan.getStatus() != expected) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
    }

    private void requireCreator(ItemAllocationPlan plan) {
        String actor = securityUtil.getCurrentUserId();
        if (actor == null || !actor.equals(plan.getCreateBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED.name());
        }
    }

    private SupportItem requireItem(String itemId) {
        return supportItemRepository.findById(itemId)
                .filter(i -> !i.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
    }

    private void validateLineSum(ItemPlanRequest request) {
        Set<Integer> seen = new HashSet<>();
        int sum = 0;
        for (ItemPlanLineRequest line : request.getLines()) {
            if (!seen.add(line.getBeneficiaryId())) {
                throw badRequest(ErrorCode.ARGUMENT_INVALID); // duplicate beneficiary in the same plan
            }
            sum += line.getPlannedQty();
        }
        if (sum != request.getPlannedQty()) {
            throw badRequest(ErrorCode.LINE_SUM_MISMATCH);
        }
    }

    private void saveLines(ItemAllocationPlan plan, List<ItemPlanLineRequest> lines) {
        for (ItemPlanLineRequest req : lines) {
            Benificiary beneficiary = benificiaryRepository.findById(req.getBeneficiaryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, ErrorCode.BENIFICIARY_NOT_FOUND.name()));
            lineRepository.save(ItemAllocationPlanLine.builder()
                    .plan(plan)
                    .beneficiary(beneficiary)
                    .plannedQty(req.getPlannedQty())
                    .issuedQty(0)
                    .lineStatus(PlanLineStatus.PENDING)
                    .createAt(LocalDate.now())
                    .createBy(plan.getCreateBy())
                    .build());
        }
    }

    private GoodsInventory lockInventory(String itemId) {
        goodsInventoryRepository.ensureRow(itemId);
        return goodsInventoryRepository.lockByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
    }

    private void requireWithinAvailable(String itemId, int plannedQty) {
        int available = goodsInventoryRepository.findByItem_Id(itemId)
                .map(this::available).orElse(0);
        if (plannedQty > available) {
            throw badRequest(ErrorCode.STOCK_INSUFFICIENT);
        }
    }

    private int available(GoodsInventory inv) {
        return inv.getQuantityOnHand() - inv.getReservedQuantity();
    }

    private void requireFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw badRequest(ErrorCode.EVIDENCE_REQUIRED);
        }
    }

    private String requireReason(ReasonRequest request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw badRequest(ErrorCode.REASON_REQUIRED);
        }
        return request.getReason().trim();
    }

    /** Auto-COMPLETE the header once no PENDING line remains. */
    private void recomputeCompletion(ItemAllocationPlan plan) {
        if (plan.getStatus() == PlanStatus.APPROVED
                && lineRepository.countByPlanIdAndLineStatus(plan.getId(), PlanLineStatus.PENDING) == 0) {
            plan.setStatus(PlanStatus.COMPLETED);
            planRepository.save(plan);
        }
    }

    /** After the approve commits, notify each line's beneficiary (if linked to a user account). */
    private void scheduleNotifications(ItemAllocationPlan plan) {
        String itemName = plan.getItem() != null ? plan.getItem().getName() : "";
        String window = plan.getDeliveryTimeWindow() != null ? " " + plan.getDeliveryTimeWindow() : "";
        List<Runnable> sends = new ArrayList<>();
        for (ItemAllocationPlanLine line : lineRepository.findByPlanId(plan.getId())) {
            User user = resolveBeneficiaryUser(line.getBeneficiary());
            if (user == null) {
                continue;
            }
            String message = "Bạn có hỗ trợ " + itemName + " × " + line.getPlannedQty()
                    + ", nhận tại " + plan.getDeliveryPlace() + window + " ngày " + plan.getExpectedDate();
            String planId = plan.getId();
            sends.add(() -> notificationService.sendNotification(user, NotificationType.APPROVAL,
                    "Thông báo nhận hỗ trợ", message, "ITEM_ALLOCATION_PLAN", planId, null, null));
        }
        if (sends.isEmpty()) {
            return;
        }
        Runnable dispatch = () -> {
            for (Runnable send : sends) {
                try {
                    send.run();
                } catch (Exception e) {
                    log.error("Failed to send allocation notification: {}", e.getMessage());
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private User resolveBeneficiaryUser(Benificiary beneficiary) {
        if (beneficiary == null || beneficiary.getApplication() == null) {
            return null;
        }
        return beneficiary.getApplication().getSubmitBy();
    }

    private ItemPlanDetailResponse toDetail(ItemAllocationPlan plan) {
        List<ItemPlanLineResponse> lines = lineRepository.findByPlanId(plan.getId()).stream()
                .map(ItemPlanLineResponse::from).toList();
        List<AttachmentResponse> listFiles =
                resourceAttachmentService.listByKind(Table.ITEM_ALLOCATION_PLAN, plan.getId(), AttachmentKind.LIST);
        return ItemPlanDetailResponse.from(plan, lines, listFiles, buildTimeline(plan));
    }

    private List<PlanTimelineEntry> buildTimeline(ItemAllocationPlan plan) {
        List<PlanTimelineEntry> timeline = new ArrayList<>();
        timeline.add(PlanTimelineEntry.builder()
                .status(PlanStatus.PENDING_APPROVAL.name()).by(plan.getCreateBy()).build());
        if (plan.getApprovedAt() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.APPROVED.name()).by(plan.getApprovedBy()).at(plan.getApprovedAt()).build());
        }
        if (plan.getRejectReason() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.REJECTED.name()).reason(plan.getRejectReason()).build());
        }
        if (plan.getCancelReason() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.CANCELLED.name()).reason(plan.getCancelReason()).build());
        }
        if (plan.getDeletedAt() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.DELETED.name()).by(plan.getDeletedBy())
                    .at(plan.getDeletedAt()).reason(plan.getDeleteReason()).build());
        }
        return timeline;
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
