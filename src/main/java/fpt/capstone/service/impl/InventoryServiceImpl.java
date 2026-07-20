package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.StockAdjustmentRequest;
import fpt.capstone.dto.response.InventoryBalanceResponse;
import fpt.capstone.dto.response.InventoryTransactionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Distribution;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.entity.StockAdjustment;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.ReceiptStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.DistributionRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.InboundReceiptRepository;
import fpt.capstone.repository.StockAdjustmentRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.InventoryService;
import fpt.capstone.service.ResourceAttachmentService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "updatedAt", "updateAt",
            "quantityOnHand", "quantityOnHand");

    private final GoodsInventoryRepository goodsInventoryRepository;
    private final SupportItemRepository supportItemRepository;
    private final InboundReceiptRepository inboundReceiptRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final DistributionRepository distributionRepository;
    private final ResourceAttachmentService resourceAttachmentService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryBalanceResponse> listBalances(int page, int size, String sort, String dir) {
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
        Page<GoodsInventory> result = goodsInventoryRepository.findByIsDeleteFalse(pageable);
        return PageResponse.from(result.map(InventoryBalanceResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> transactions(String itemId, int page, int size) {
        if (page < 0 || size < 1) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        requireItem(itemId);
        List<InventoryTransactionResponse> ledger = new ArrayList<>();
        // Inbound (+): only POSTED receipts affect stock.
        for (InboundReceipt r : inboundReceiptRepository.findByItem_IdAndStatus(itemId, ReceiptStatus.POSTED)) {
            ledger.add(InventoryTransactionResponse.builder()
                    .id(r.getId()).type("INBOUND").refCode(r.getCode())
                    .delta(r.getQuantity()).occurredAt(r.getPostedAt()).performedBy(r.getPostedBy())
                    .build());
        }
        // Adjustments (±): signed delta with the resulting balance.
        for (StockAdjustment a : stockAdjustmentRepository.findByItem_Id(itemId)) {
            ledger.add(InventoryTransactionResponse.builder()
                    .id(a.getId()).type("ADJUSTMENT")
                    .delta(a.getDeltaQty()).reason(a.getReason()).balanceAfter(a.getBalanceAfter())
                    .occurredAt(a.getAdjustedAt()).performedBy(a.getAdjustedBy())
                    .build());
        }
        // Distribution (−): goods issued to beneficiaries.
        for (Distribution d : distributionRepository.findByItemId(itemId)) {
            ledger.add(InventoryTransactionResponse.builder()
                    .id(d.getId()).type("DISTRIBUTION").refCode(d.getCode())
                    .delta(-d.getActualQty())
                    .occurredAt(d.getIssueDate() != null ? d.getIssueDate().atStartOfDay() : null)
                    .performedBy(d.getIssuingOfficer())
                    .build());
        }
        ledger.sort(Comparator.comparing(InventoryTransactionResponse::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int total = ledger.size();
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        int from = Math.min(page * cappedSize, total);
        int to = Math.min(from + cappedSize, total);
        List<InventoryTransactionResponse> pageItems = ledger.subList(from, to);
        return PageResponse.<InventoryTransactionResponse>builder()
                .items(pageItems)
                .page(page)
                .size(cappedSize)
                .totalElements(total)
                .totalPages(cappedSize == 0 ? 0 : (int) Math.ceil((double) total / cappedSize))
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = Action.STOCK_ADJUST, entity = Table.STOCK_ADJUSTMENT)
    public InventoryTransactionResponse adjust(String itemId, StockAdjustmentRequest request,
                                               List<MultipartFile> files) {
        if (request.getDeltaQty() == null || request.getDeltaQty() == 0) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        SupportItem item = requireItem(itemId);
        goodsInventoryRepository.ensureRow(itemId);
        GoodsInventory inv = goodsInventoryRepository.lockByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
        if (inv.getItem() == null) {
            inv.setItem(item);
        }
        int newOnHand = inv.getQuantityOnHand() + request.getDeltaQty();
        if (newOnHand < 0 || newOnHand < inv.getReservedQuantity()) {
            throw badRequest(ErrorCode.STOCK_INSUFFICIENT);
        }
        inv.setQuantityOnHand(newOnHand);
        inv.setUpdateAt(LocalDate.now());
        goodsInventoryRepository.save(inv);

        String actor = securityUtil.getCurrentUserId();
        StockAdjustment adjustment = StockAdjustment.builder()
                .item(item)
                .deltaQty(request.getDeltaQty())
                .reason(request.getReason())
                .balanceAfter(newOnHand)
                .adjustedBy(actor)
                .adjustedAt(LocalDateTime.now())
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        adjustment = stockAdjustmentRepository.save(adjustment);
        if (files != null && !files.isEmpty()) {
            resourceAttachmentService.saveAll(Table.STOCK_ADJUSTMENT, adjustment.getId(),
                    AttachmentKind.EVIDENCE, files);
        }
        return InventoryTransactionResponse.builder()
                .id(adjustment.getId()).type("ADJUSTMENT")
                .delta(adjustment.getDeltaQty()).reason(adjustment.getReason())
                .balanceAfter(adjustment.getBalanceAfter())
                .occurredAt(adjustment.getAdjustedAt()).performedBy(adjustment.getAdjustedBy())
                .build();
    }

    private SupportItem requireItem(String itemId) {
        return supportItemRepository.findById(itemId)
                .filter(i -> !i.isDelete())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND.name()));
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
