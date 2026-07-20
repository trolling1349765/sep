package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.FundingCreateRequest;
import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.dto.response.FundingDetailResponse;
import fpt.capstone.dto.response.FundingListResponse;
import fpt.capstone.dto.response.FundingPlanSummary;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PaymentMethod;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.FundUsagePlanRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.FundingService;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl implements FundingService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "receivedDate", "transferDate",
            "amount", "amount");

    private final DonationRepository donationRepository;
    private final SponsorRepository sponsorRepository;
    private final FundUsagePlanRepository fundUsagePlanRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FundingListResponse> search(int page, int size, FundingStatus status, String sponsorId,
                                                    LocalDate fromDate, LocalDate toDate, String sort, String dir) {
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
        Page<Donation> result = donationRepository.search(status, sponsorId, fromDate, toDate, pageable);
        return PageResponse.from(result.map(FundingListResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public FundingDetailResponse getById(int id) {
        return toDetail(requireFunding(id));
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUNDING_CREATE, entity = Table.DONATION)
    public FundingDetailResponse create(FundingCreateRequest request, List<MultipartFile> files) {
        Sponsor sponsor = resolveSponsor(request.getSponsorId());
        requireTransactionRef(request);

        boolean hasFiles = files != null && !files.isEmpty();
        String actor = securityUtil.getCurrentUserId();
        Donation donation = Donation.builder()
                .code(codeGenerator.next("KP"))
                .name(request.getName())
                .sponsor(sponsor)
                .amount(request.getAmount())
                .pendingAmount(BigDecimal.ZERO)
                .spentAmount(BigDecimal.ZERO)
                .purpose(request.getPurpose())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .evidenceName(request.getEvidenceName())
                .status(hasFiles ? FundingStatus.CONFIRMED : FundingStatus.DRAFT)
                .recordedBy(actor)
                .transferDate(request.getReceivedDate())
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        donation = donationRepository.save(donation);
        if (hasFiles) {
            resourceAttachmentService.saveAll(Table.DONATION, String.valueOf(donation.getId()),
                    AttachmentKind.EVIDENCE, files);
        }
        return toDetail(donation);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUNDING_UPDATE, entity = Table.DONATION)
    public FundingDetailResponse update(int id, FundingCreateRequest request, List<MultipartFile> files) {
        Donation donation = requireFunding(id);
        if (donation.getStatus() != FundingStatus.DRAFT) {
            throw badRequest(ErrorCode.FUNDING_LOCKED);
        }
        Sponsor sponsor = resolveSponsor(request.getSponsorId());
        requireTransactionRef(request);

        donation.setName(request.getName());
        donation.setSponsor(sponsor);
        donation.setAmount(request.getAmount());
        donation.setPurpose(request.getPurpose());
        donation.setPaymentMethod(request.getPaymentMethod());
        donation.setTransactionRef(request.getTransactionRef());
        donation.setEvidenceName(request.getEvidenceName());
        donation.setTransferDate(request.getReceivedDate());
        donation.setUpdateAt(LocalDate.now());
        donation.setUpdateBy(securityUtil.getCurrentUserId());
        donation = donationRepository.save(donation);
        if (files != null && !files.isEmpty()) {
            resourceAttachmentService.saveAll(Table.DONATION, String.valueOf(donation.getId()),
                    AttachmentKind.EVIDENCE, files);
        }
        return toDetail(donation);
    }

    @Override
    @Transactional
    @Auditable(action = Action.FUNDING_CONFIRM, entity = Table.DONATION)
    public FundingDetailResponse confirm(int id) {
        Donation donation = requireFunding(id);
        if (donation.getStatus() == FundingStatus.CONFIRMED) {
            throw badRequest(ErrorCode.FUNDING_LOCKED);
        }
        List<AttachmentResponse> evidence =
                resourceAttachmentService.listByKind(Table.DONATION, String.valueOf(id), AttachmentKind.EVIDENCE);
        if (evidence.isEmpty()) {
            throw badRequest(ErrorCode.EVIDENCE_REQUIRED);
        }
        donation.setStatus(FundingStatus.CONFIRMED);
        donation.setUpdateAt(LocalDate.now());
        donation.setUpdateBy(securityUtil.getCurrentUserId());
        donation = donationRepository.save(donation);
        return toDetail(donation);
    }

    // ---- helpers ----

    private Donation requireFunding(int id) {
        return donationRepository.findById(id)
                .filter(d -> !d.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.FUNDING_NOT_FOUND.name()));
    }

    /** Optional sponsor link; when present it must reference an ACTIVE, non-deleted sponsor. */
    private Sponsor resolveSponsor(String sponsorId) {
        if (sponsorId == null || sponsorId.isBlank()) {
            return null;
        }
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .filter(s -> !s.isDelete() && s.getStatus() == SponsorStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.SPONSOR_NOT_FOUND.name()));
        return sponsor;
    }

    private void requireTransactionRef(FundingCreateRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.TRANSFER
                && (request.getTransactionRef() == null || request.getTransactionRef().isBlank())) {
            throw badRequest(ErrorCode.TRANSACTION_REF_REQUIRED);
        }
    }

    private FundingDetailResponse toDetail(Donation donation) {
        List<AttachmentResponse> attachments =
                resourceAttachmentService.list(Table.DONATION, String.valueOf(donation.getId()));
        List<FundingPlanSummary> plans = fundUsagePlanRepository.findByDonationId(donation.getId()).stream()
                .filter(p -> p.getStatus() != PlanStatus.DELETED)
                .map(p -> FundingPlanSummary.builder()
                        .code(p.getCode())
                        .amount(p.getAmount())
                        .status(p.getStatus() != null ? p.getStatus().name() : null)
                        .createdAt(p.getCreateAt())
                        .build())
                .toList();
        return FundingDetailResponse.from(donation, attachments, plans);
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
