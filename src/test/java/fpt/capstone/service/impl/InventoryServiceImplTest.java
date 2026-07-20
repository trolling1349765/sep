package fpt.capstone.service.impl;

import fpt.capstone.dto.request.StockAdjustmentRequest;
import fpt.capstone.dto.response.InventoryTransactionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.entity.StockAdjustment;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.ReceiptStatus;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.InboundReceiptRepository;
import fpt.capstone.repository.StockAdjustmentRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private GoodsInventoryRepository goodsInventoryRepository;
    @Mock private SupportItemRepository supportItemRepository;
    @Mock private InboundReceiptRepository inboundReceiptRepository;
    @Mock private StockAdjustmentRepository stockAdjustmentRepository;
    @Mock private fpt.capstone.repository.DistributionRepository distributionRepository;
    @Mock private ResourceAttachmentService resourceAttachmentService;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private InventoryServiceImpl service;

    private SupportItem item() {
        return SupportItem.builder().id("i-1").code("VP-2026-001").name("Gao").unit("kg").build();
    }

    @BeforeEach
    void stubs() {
        lenient().when(distributionRepository.findByItemId(any())).thenReturn(List.of());
        lenient().when(stockAdjustmentRepository.save(any()))
                .thenAnswer(inv -> {
                    StockAdjustment a = inv.getArgument(0, StockAdjustment.class);
                    a.setId("adj-1");
                    return a;
                });
        lenient().when(securityUtil.getCurrentUserId()).thenReturn("user-1");
    }

    @Test
    void adjust_zeroDelta_shouldThrowArgumentInvalid() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.adjust("i-1", StockAdjustmentRequest.builder().deltaQty(0).reason("x").build(), null));
        assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
    }

    @Test
    void adjust_goingNegative_shouldThrowStockInsufficient() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        GoodsInventory inv = GoodsInventory.builder().item(item()).quantityOnHand(10).reservedQuantity(0).build();
        when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.adjust("i-1", StockAdjustmentRequest.builder().deltaQty(-20).reason("hao hut").build(), null));
        assertEquals(ErrorCode.STOCK_INSUFFICIENT.name(), ex.getReason());
    }

    @Test
    void adjust_belowReserved_shouldThrowStockInsufficient() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        GoodsInventory inv = GoodsInventory.builder().item(item()).quantityOnHand(30).reservedQuantity(25).build();
        when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv));

        // 30 - 10 = 20 < reserved 25 -> insufficient
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.adjust("i-1", StockAdjustmentRequest.builder().deltaQty(-10).reason("x").build(), null));
        assertEquals(ErrorCode.STOCK_INSUFFICIENT.name(), ex.getReason());
    }

    @Test
    void adjust_valid_shouldSetBalanceAndRecordAdjustment() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        GoodsInventory inv = GoodsInventory.builder().item(item()).quantityOnHand(100).reservedQuantity(0).build();
        when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv));

        InventoryTransactionResponse res = service.adjust("i-1",
                StockAdjustmentRequest.builder().deltaQty(50).reason("kiem ke").build(), null);

        assertEquals(150, inv.getQuantityOnHand());
        assertEquals(150, res.getBalanceAfter());
        assertEquals(50, res.getDelta());
        verify(goodsInventoryRepository).ensureRow("i-1");
        verify(goodsInventoryRepository).save(inv);
    }

    @Test
    void transactions_shouldMergeInboundAndAdjustmentsSortedDesc() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        InboundReceipt r = InboundReceipt.builder().id("r-1").code("PNK-2026-001").quantity(1000)
                .status(ReceiptStatus.POSTED).postedAt(LocalDateTime.of(2026, 1, 1, 8, 0)).build();
        StockAdjustment a = StockAdjustment.builder().id("adj-1").deltaQty(-5).reason("hao")
                .balanceAfter(995).adjustedAt(LocalDateTime.of(2026, 2, 1, 9, 0)).build();
        when(inboundReceiptRepository.findByItem_IdAndStatus("i-1", ReceiptStatus.POSTED)).thenReturn(List.of(r));
        when(stockAdjustmentRepository.findByItem_Id("i-1")).thenReturn(List.of(a));

        PageResponse<InventoryTransactionResponse> page = service.transactions("i-1", 0, 20);

        assertEquals(2, page.getTotalElements());
        // newest first: the Feb adjustment before the Jan inbound
        assertEquals("ADJUSTMENT", page.getItems().get(0).getType());
        assertEquals("INBOUND", page.getItems().get(1).getType());
    }
}
