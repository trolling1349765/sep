package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.InboundReceiptRequest;
import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.dto.response.InboundReceiptDetailResponse;
import fpt.capstone.dto.response.InboundReceiptListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.ReceiptStatus;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.InboundReceiptRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.InboundReceiptService;
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
public class InboundReceiptServiceImpl implements InboundReceiptService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "createdAt", "createAt",
            "receiveDate", "receiveDate");

    private final InboundReceiptRepository inboundReceiptRepository;
    private final SupportItemRepository supportItemRepository;
    private final SponsorRepository sponsorRepository;
    private final GoodsInventoryRepository goodsInventoryRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundReceiptListResponse> search(int page, int size, String q, ReceiptStatus status,
                                                           String itemId, String sort, String dir) {
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
        Page<InboundReceipt> result = inboundReceiptRepository.search(query, status, itemId, pageable);
        return PageResponse.from(result.map(InboundReceiptListResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public InboundReceiptDetailResponse getById(String id) {
        return toDetail(requireReceipt(id));
    }

    @Override
    @Transactional
    @Auditable(action = Action.RECEIPT_CREATE, entity = Table.INBOUND_RECEIPT)
    public InboundReceiptDetailResponse create(InboundReceiptRequest request, List<MultipartFile> files) {
        SupportItem item = requireItem(request.getItemId());
        Sponsor sponsor = resolveSponsor(request.getSponsorId());
        requireFiles(files);

        String actor = securityUtil.getCurrentUserId();
        InboundReceipt receipt = InboundReceipt.builder()
                .code(codeGenerator.next("PNK"))
                .item(item)
                .sponsor(sponsor)
                .delivererName(request.getDelivererName())
                .quantity(request.getQuantity())
                .condition(request.getCondition())
                .receiveDate(request.getReceiveDate())
                .evidenceName(request.getEvidenceName())
                .status(ReceiptStatus.DRAFT)
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        receipt = inboundReceiptRepository.save(receipt);
        resourceAttachmentService.saveAll(Table.INBOUND_RECEIPT, receipt.getId(), AttachmentKind.EVIDENCE, files);
        return toDetail(receipt);
    }

    @Override
    @Transactional
    @Auditable(action = Action.RECEIPT_UPDATE, entity = Table.INBOUND_RECEIPT)
    public InboundReceiptDetailResponse update(String id, InboundReceiptRequest request, List<MultipartFile> files) {
        InboundReceipt receipt = requireReceipt(id);
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw badRequest(ErrorCode.RECEIPT_LOCKED);
        }
        SupportItem item = requireItem(request.getItemId());
        Sponsor sponsor = resolveSponsor(request.getSponsorId());

        receipt.setItem(item);
        receipt.setSponsor(sponsor);
        receipt.setDelivererName(request.getDelivererName());
        receipt.setQuantity(request.getQuantity());
        receipt.setCondition(request.getCondition());
        receipt.setReceiveDate(request.getReceiveDate());
        receipt.setEvidenceName(request.getEvidenceName());
        receipt.setUpdateAt(LocalDate.now());
        receipt.setUpdateBy(securityUtil.getCurrentUserId());
        receipt = inboundReceiptRepository.save(receipt);
        if (files != null && !files.isEmpty()) {
            resourceAttachmentService.saveAll(Table.INBOUND_RECEIPT, receipt.getId(), AttachmentKind.EVIDENCE, files);
        }
        return toDetail(receipt);
    }

    @Override
    @Transactional
    @Auditable(action = Action.RECEIPT_POST, entity = Table.INBOUND_RECEIPT)
    public InboundReceiptDetailResponse post(String id) {
        InboundReceipt receipt = requireReceipt(id);
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw badRequest(ErrorCode.RECEIPT_ALREADY_POSTED);
        }
        String itemId = receipt.getItem().getId();
        // Idempotent create-if-absent, then lock the balance row and add on-hand.
        goodsInventoryRepository.ensureRow(itemId);
        GoodsInventory inv = goodsInventoryRepository.lockByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
        if (inv.getItem() == null) {
            inv.setItem(receipt.getItem());
        }
        inv.setQuantityOnHand(inv.getQuantityOnHand() + receipt.getQuantity());
        inv.setUpdateAt(LocalDate.now());
        goodsInventoryRepository.save(inv);

        String actor = securityUtil.getCurrentUserId();
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setPostedBy(actor);
        receipt.setPostedAt(LocalDateTime.now());
        receipt = inboundReceiptRepository.save(receipt);
        return toDetail(receipt);
    }

    // ---- helpers ----

    private InboundReceipt requireReceipt(String id) {
        return inboundReceiptRepository.findById(id)
                .filter(r -> !r.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.RECEIPT_NOT_FOUND.name()));
    }

    private SupportItem requireItem(String itemId) {
        return supportItemRepository.findById(itemId)
                .filter(i -> !i.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
    }

    private Sponsor resolveSponsor(String sponsorId) {
        if (sponsorId == null || sponsorId.isBlank()) {
            return null;
        }
        return sponsorRepository.findById(sponsorId)
                .filter(s -> !s.isDelete() && s.getStatus() == SponsorStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.SPONSOR_NOT_FOUND.name()));
    }

    private void requireFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw badRequest(ErrorCode.EVIDENCE_REQUIRED);
        }
    }

    private InboundReceiptDetailResponse toDetail(InboundReceipt receipt) {
        List<AttachmentResponse> attachments =
                resourceAttachmentService.list(Table.INBOUND_RECEIPT, receipt.getId());
        return InboundReceiptDetailResponse.from(receipt, attachments);
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
