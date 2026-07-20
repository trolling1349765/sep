package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.request.UpdateSponsorStatusRequest;
import fpt.capstone.dto.response.ContributionHistoryItem;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SponsorDetailResponse;
import fpt.capstone.dto.response.SponsorListResponse;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ContributionKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import fpt.capstone.enums.Table;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.InboundReceiptRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.SponsorService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import fpt.capstone.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SponsorServiceImpl implements SponsorService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int HISTORY_LIMIT = 100;

    // API sort key -> entity property (sort-injection guard).
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "name", "name",
            "code", "code");

    private final SponsorRepository sponsorRepository;
    private final DonationRepository donationRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SponsorListResponse> search(int page, int size, String q,
                                                    SponsorType type, SponsorStatus status,
                                                    String sort, String dir) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        String property = SORT_WHITELIST.get(sort);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }

        String query = (q == null || q.isBlank()) ? null : q.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));

        Page<Sponsor> result = sponsorRepository.search(query, type, status, pageable);
        return PageResponse.from(result.map(SponsorListResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public SponsorDetailResponse getById(String id) {
        Sponsor sponsor = requireSponsor(id);
        return SponsorDetailResponse.from(sponsor, buildHistory(sponsor));
    }

    @Override
    @Transactional
    @Auditable(action = Action.SPONSOR_CREATE, entity = Table.SPONSOR)
    public SponsorDetailResponse create(SponsorRequest request) {
        String normalizedName = TextNormalizer.normalize(request.getName());
        checkDuplicate(request, normalizedName, null);

        String actor = securityUtil.getCurrentUserId();
        Sponsor sponsor = Sponsor.builder()
                .code(codeGenerator.next("NTT"))
                .name(request.getName())
                .normalizedName(normalizedName)
                .type(request.getType())
                .orgCode(blankToNull(request.getOrgCode()))
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .note(request.getNote())
                .status(SponsorStatus.ACTIVE)
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        sponsor = sponsorRepository.save(sponsor);
        return SponsorDetailResponse.from(sponsor, List.of());
    }

    @Override
    @Transactional
    @Auditable(action = Action.SPONSOR_UPDATE, entity = Table.SPONSOR)
    public SponsorDetailResponse update(String id, SponsorRequest request) {
        Sponsor sponsor = requireSponsor(id);
        String normalizedName = TextNormalizer.normalize(request.getName());
        checkDuplicate(request, normalizedName, id);

        sponsor.setName(request.getName());
        sponsor.setNormalizedName(normalizedName);
        sponsor.setType(request.getType());
        sponsor.setOrgCode(blankToNull(request.getOrgCode()));
        sponsor.setContactPerson(request.getContactPerson());
        sponsor.setPhone(request.getPhone());
        sponsor.setEmail(request.getEmail());
        sponsor.setAddress(request.getAddress());
        sponsor.setNote(request.getNote());
        sponsor.setUpdateAt(LocalDate.now());
        sponsor.setUpdateBy(securityUtil.getCurrentUserId());
        sponsor = sponsorRepository.save(sponsor);
        return SponsorDetailResponse.from(sponsor, buildHistory(sponsor));
    }

    @Override
    @Transactional
    @Auditable(action = Action.SPONSOR_STATUS_CHANGE, entity = Table.SPONSOR)
    public SponsorDetailResponse changeStatus(String id, UpdateSponsorStatusRequest request) {
        Sponsor sponsor = requireSponsor(id);
        SponsorStatus target = parseStatus(request.getStatus());
        if (target == sponsor.getStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATUS.name());
        }
        sponsor.setStatus(target);
        sponsor.setUpdateAt(LocalDate.now());
        sponsor.setUpdateBy(securityUtil.getCurrentUserId());
        sponsor = sponsorRepository.save(sponsor);
        return SponsorDetailResponse.from(sponsor, buildHistory(sponsor));
    }

    // ---- helpers ----

    private Sponsor requireSponsor(String id) {
        return sponsorRepository.findById(id)
                .filter(s -> !s.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.SPONSOR_NOT_FOUND.name()));
    }

    private SponsorStatus parseStatus(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATUS.name());
        }
        try {
            return SponsorStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATUS.name());
        }
    }

    /**
     * BR-47: reject when another sponsor shares orgCode (when provided), phone, or
     * normalized name. The 400 body carries the duplicate's id/code/name so the FE
     * can offer to open the existing profile (branch A1/5.7.1).
     */
    private void checkDuplicate(SponsorRequest request, String normalizedName, String excludeId) {
        List<Sponsor> dups = sponsorRepository.findDuplicates(
                blankToNull(request.getOrgCode()), request.getPhone(), normalizedName, excludeId);
        if (dups.isEmpty()) {
            return;
        }
        Sponsor dup = dups.get(0);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("duplicateId", dup.getId());
        data.put("duplicateCode", dup.getCode());
        data.put("duplicateName", dup.getName());
        throw new InvalidArgsException(APIResponse.builder()
                .code(ErrorCode.SPONSOR_DUPLICATED.getCode())
                .message(ErrorCode.SPONSOR_DUPLICATED.getMessage())
                .data(data)
                .build());
    }

    private List<ContributionHistoryItem> buildHistory(Sponsor sponsor) {
        // Union of FUNDING (Donation) and ITEM (InboundReceipt) contributions, newest first.
        List<ContributionHistoryItem> history = new java.util.ArrayList<>();
        for (var d : donationRepository.findBySponsorId(sponsor.getId())) {
            history.add(ContributionHistoryItem.builder()
                    .kind(ContributionKind.FUNDING)
                    .code(d.getCode())
                    .date(d.getTransferDate())
                    .description(d.getName())
                    .amount(d.getAmount())
                    .status(d.getStatus() != null ? d.getStatus().name() : null)
                    .build());
        }
        for (var r : inboundReceiptRepository.findBySponsorIdAndIsDeleteFalse(sponsor.getId())) {
            String item = r.getItem() != null ? r.getItem().getName() : "";
            history.add(ContributionHistoryItem.builder()
                    .kind(ContributionKind.ITEM)
                    .code(r.getCode())
                    .date(r.getReceiveDate())
                    .description(item + " x" + r.getQuantity())
                    .status(r.getStatus() != null ? r.getStatus().name() : null)
                    .build());
        }
        return history.stream()
                .sorted(Comparator.comparing(ContributionHistoryItem::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(HISTORY_LIMIT)
                .toList();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
