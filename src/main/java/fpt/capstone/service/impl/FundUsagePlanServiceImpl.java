package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.FundPlanRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.dto.response.FundPlanDetailResponse;
import fpt.capstone.dto.response.FundPlanListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.PlanTimelineEntry;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.FundUsagePlan;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.FundUsagePlanRepository;
import fpt.capstone.service.FundUsagePlanService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FundUsagePlanServiceImpl implements FundUsagePlanService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "expectedDate", "expectedDate",
            "amount", "amount");

    private final FundUsagePlanRepository planRepository;
    private final DonationRepository donationRepository;
    private final BenificiaryRepository benificiaryRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FundPlanListResponse> search(int page, int size, String q, Integer donationId,
                                                     PlanStatus status, Integer beneficiaryId,
                                                     String sort, String dir) {
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
        Page<FundUsagePlan> result = planRepository.search(query, donationId, status, beneficiaryId, pageable);
        return PageResponse.from(result.map(FundPlanListResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public FundPlanDetailResponse getById(String id) {
        return toDetail(requirePlan(id));
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_CREATE, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse create(FundPlanRequest request, List<MultipartFile> files) {
        Benificiary beneficiary = validateTargetAndResolve(request);
        Donation donation = requireConfirmedDonation(request.getDonationId());
        requireWithinAvailable(donation, request.getAmount());
        requireFiles(files);

        String actor = securityUtil.getCurrentUserId();
        FundUsagePlan plan = FundUsagePlan.builder()
                .code(codeGenerator.next("KH"))
                .donation(donation)
                .beneficiary(beneficiary)
                .programName(blankToNull(request.getProgramName()))
                .amount(request.getAmount())
                .purpose(request.getPurpose())
                .expectedDate(request.getExpectedDate())
                .deliveryPlace(request.getDeliveryPlace())
                .status(PlanStatus.PENDING_APPROVAL)
                .submittedAt(LocalDateTime.now())
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        plan = planRepository.save(plan);
        resourceAttachmentService.saveAll(Table.FUND_USAGE_PLAN, plan.getId(), AttachmentKind.LIST, files);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_UPDATE, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse update(String id, FundPlanRequest request, List<MultipartFile> files) {
        FundUsagePlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.REJECTED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        Benificiary beneficiary = validateTargetAndResolve(request);
        Donation donation = requireConfirmedDonation(request.getDonationId());
        requireWithinAvailable(donation, request.getAmount());

        plan.setDonation(donation);
        plan.setBeneficiary(beneficiary);
        plan.setProgramName(blankToNull(request.getProgramName()));
        plan.setAmount(request.getAmount());
        plan.setPurpose(request.getPurpose());
        plan.setExpectedDate(request.getExpectedDate());
        plan.setDeliveryPlace(request.getDeliveryPlace());
        // Editing a rejected plan resubmits it for approval.
        if (plan.getStatus() == PlanStatus.REJECTED) {
            plan.setStatus(PlanStatus.PENDING_APPROVAL);
            plan.setRejectReason(null);
            plan.setSubmittedAt(LocalDateTime.now());
        }
        plan.setUpdateAt(LocalDate.now());
        plan.setUpdateBy(securityUtil.getCurrentUserId());
        plan = planRepository.save(plan);
        if (files != null && !files.isEmpty()) {
            resourceAttachmentService.saveAll(Table.FUND_USAGE_PLAN, plan.getId(), AttachmentKind.LIST, files);
        }
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_APPROVE, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse approve(String id) {
        FundUsagePlan plan = requirePlan(id);
        String actor = securityUtil.getCurrentUserId();
        if (actor != null && actor.equals(plan.getCreateBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.SELF_APPROVAL_FORBIDDEN.name());
        }
        requireStatus(plan, PlanStatus.PENDING_APPROVAL);
        // Lock the donation row and re-check availability before reserving.
        Donation donation = lockDonation(plan.getDonation().getId());
        requireWithinAvailable(donation, plan.getAmount());
        donation.setPendingAmount(nz(donation.getPendingAmount()).add(plan.getAmount()));
        donationRepository.save(donation);

        plan.setStatus(PlanStatus.APPROVED);
        plan.setApprovedBy(actor);
        plan.setApprovedAt(LocalDateTime.now());
        plan = planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_REJECT, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse reject(String id, ReasonRequest request) {
        FundUsagePlan plan = requirePlan(id);
        requireStatus(plan, PlanStatus.PENDING_APPROVAL);
        String reason = requireReason(request);
        plan.setStatus(PlanStatus.REJECTED);
        plan.setRejectReason(reason);
        plan = planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_CANCEL, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse cancel(String id, ReasonRequest request) {
        FundUsagePlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.APPROVED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        String reason = requireReason(request);
        if (plan.getStatus() == PlanStatus.APPROVED) {
            Donation donation = lockDonation(plan.getDonation().getId());
            donation.setPendingAmount(nz(donation.getPendingAmount()).subtract(plan.getAmount()));
            donationRepository.save(donation);
        }
        plan.setStatus(PlanStatus.CANCELLED);
        plan.setCancelReason(reason);
        plan = planRepository.save(plan);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_COMPLETE, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse complete(String id, List<MultipartFile> files) {
        FundUsagePlan plan = requirePlan(id);
        requireCreator(plan);
        requireStatus(plan, PlanStatus.APPROVED);
        requireFiles(files);

        Donation donation = lockDonation(plan.getDonation().getId());
        donation.setPendingAmount(nz(donation.getPendingAmount()).subtract(plan.getAmount()));
        donation.setSpentAmount(nz(donation.getSpentAmount()).add(plan.getAmount()));
        donationRepository.save(donation);

        plan.setStatus(PlanStatus.COMPLETED);
        plan.setCompletedAt(LocalDateTime.now());
        plan = planRepository.save(plan);
        resourceAttachmentService.saveAll(Table.FUND_USAGE_PLAN, plan.getId(), AttachmentKind.COMPLETION, files);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUND_PLAN_DELETE, entity = Table.FUND_USAGE_PLAN)
    public FundPlanDetailResponse delete(String id, ReasonRequest request) {
        FundUsagePlan plan = requirePlan(id);
        requireCreator(plan);
        if (plan.getStatus() != PlanStatus.PENDING_APPROVAL && plan.getStatus() != PlanStatus.REJECTED) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
        String reason = requireReason(request);
        plan.setStatus(PlanStatus.DELETED);
        plan.setDeleteReason(reason);
        plan.setDeletedBy(securityUtil.getCurrentUserId());
        plan.setDeletedAt(LocalDateTime.now());
        plan = planRepository.save(plan);
        return toDetail(plan);
    }

    // ---- guards & helpers ----

    private FundUsagePlan requirePlan(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.PLAN_NOT_FOUND.name()));
    }

    private void requireStatus(FundUsagePlan plan, PlanStatus expected) {
        if (plan.getStatus() != expected) {
            throw badRequest(ErrorCode.PLAN_INVALID_STATE);
        }
    }

    private void requireCreator(FundUsagePlan plan) {
        String actor = securityUtil.getCurrentUserId();
        if (actor == null || !actor.equals(plan.getCreateBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED.name());
        }
    }

    /** Exactly one of beneficiaryId / programName; resolves the beneficiary when present. */
    private Benificiary validateTargetAndResolve(FundPlanRequest request) {
        boolean hasBeneficiary = request.getBeneficiaryId() != null;
        boolean hasProgram = request.getProgramName() != null && !request.getProgramName().isBlank();
        if (hasBeneficiary == hasProgram) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        if (!hasBeneficiary) {
            return null;
        }
        return benificiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.BENIFICIARY_NOT_FOUND.name()));
    }

    private Donation requireConfirmedDonation(int donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.FUNDING_NOT_FOUND.name()));
        if (donation.getStatus() != FundingStatus.CONFIRMED) {
            throw badRequest(ErrorCode.FUNDING_NOT_CONFIRMED);
        }
        return donation;
    }

    private Donation lockDonation(int donationId) {
        return donationRepository.findByIdForUpdate(donationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.FUNDING_NOT_FOUND.name()));
    }

    private void requireWithinAvailable(Donation donation, BigDecimal amount) {
        BigDecimal available = nz(donation.getAmount())
                .subtract(nz(donation.getPendingAmount()))
                .subtract(nz(donation.getSpentAmount()));
        if (amount.compareTo(available) > 0) {
            throw badRequest(ErrorCode.FUNDING_BALANCE_INSUFFICIENT);
        }
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

    private FundPlanDetailResponse toDetail(FundUsagePlan plan) {
        List<AttachmentResponse> listFiles =
                resourceAttachmentService.listByKind(Table.FUND_USAGE_PLAN, plan.getId(), AttachmentKind.LIST);
        List<AttachmentResponse> completionFiles =
                resourceAttachmentService.listByKind(Table.FUND_USAGE_PLAN, plan.getId(), AttachmentKind.COMPLETION);
        return FundPlanDetailResponse.from(plan, listFiles, completionFiles, buildTimeline(plan));
    }

    private List<PlanTimelineEntry> buildTimeline(FundUsagePlan plan) {
        List<PlanTimelineEntry> timeline = new ArrayList<>();
        timeline.add(PlanTimelineEntry.builder()
                .status(PlanStatus.PENDING_APPROVAL.name())
                .by(plan.getCreateBy())
                .at(plan.getSubmittedAt())
                .build());
        if (plan.getApprovedAt() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.APPROVED.name())
                    .by(plan.getApprovedBy())
                    .at(plan.getApprovedAt())
                    .build());
        }
        if (plan.getRejectReason() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.REJECTED.name())
                    .reason(plan.getRejectReason())
                    .build());
        }
        if (plan.getCompletedAt() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.COMPLETED.name())
                    .at(plan.getCompletedAt())
                    .build());
        }
        if (plan.getCancelReason() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.CANCELLED.name())
                    .reason(plan.getCancelReason())
                    .build());
        }
        if (plan.getDeletedAt() != null) {
            timeline.add(PlanTimelineEntry.builder()
                    .status(PlanStatus.DELETED.name())
                    .by(plan.getDeletedBy())
                    .at(plan.getDeletedAt())
                    .reason(plan.getDeleteReason())
                    .build());
        }
        return timeline;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
