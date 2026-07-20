package fpt.capstone.service.impl;

import fpt.capstone.dto.request.InboundReceiptRequest;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.ItemCondition;
import fpt.capstone.enums.ReceiptStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.InboundReceiptRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundReceiptServiceImplTest {

    @Mock private InboundReceiptRepository inboundReceiptRepository;
    @Mock private SupportItemRepository supportItemRepository;
    @Mock private SponsorRepository sponsorRepository;
    @Mock private GoodsInventoryRepository goodsInventoryRepository;
    @Mock private ResourceAttachmentService resourceAttachmentService;
    @Mock private CodeGenerator codeGenerator;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private InboundReceiptServiceImpl service;

    private SupportItem item() {
        return SupportItem.builder().id("i-1").code("VP-2026-001").name("Gao").unit("kg").build();
    }

    private InboundReceiptRequest req() {
        return InboundReceiptRequest.builder()
                .itemId("i-1").quantity(1000).condition(ItemCondition.NEW)
                .receiveDate(LocalDate.now()).evidenceName("BB").build();
    }

    private MultipartFile file() {
        return new MockMultipartFile("files", "e.pdf", "application/pdf", "x".getBytes());
    }

    @BeforeEach
    void stubs() {
        lenient().when(resourceAttachmentService.list(eq(Table.INBOUND_RECEIPT), any())).thenReturn(List.of());
        lenient().when(inboundReceiptRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0, InboundReceipt.class));
        lenient().when(codeGenerator.next("PNK")).thenReturn("PNK-2026-001");
        lenient().when(securityUtil.getCurrentUserId()).thenReturn("user-1");
    }

    @Test
    void create_itemNotFound_shouldThrow10049() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(), List.of(file())));
        assertEquals(ErrorCode.ITEM_NOT_FOUND.name(), ex.getReason());
    }

    @Test
    void create_noFiles_shouldThrowEvidenceRequired() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(), List.of()));
        assertEquals(ErrorCode.EVIDENCE_REQUIRED.name(), ex.getReason());
    }

    @Test
    void create_valid_shouldBeDraftWithoutTouchingStock() {
        when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
        service.create(req(), List.of(file()));
        verify(resourceAttachmentService).saveAll(eq(Table.INBOUND_RECEIPT), any(),
                eq(AttachmentKind.EVIDENCE), anyList());
        verify(goodsInventoryRepository, never()).ensureRow(any());
        verify(goodsInventoryRepository, never()).save(any());
    }

    @Test
    void update_posted_shouldThrowReceiptLocked() {
        InboundReceipt posted = InboundReceipt.builder().id("r-1").status(ReceiptStatus.POSTED).build();
        when(inboundReceiptRepository.findById("r-1")).thenReturn(Optional.of(posted));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update("r-1", req(), null));
        assertEquals(ErrorCode.RECEIPT_LOCKED.name(), ex.getReason());
    }

    @Test
    void post_alreadyPosted_shouldThrow10048() {
        InboundReceipt posted = InboundReceipt.builder().id("r-1").status(ReceiptStatus.POSTED).item(item()).build();
        when(inboundReceiptRepository.findById("r-1")).thenReturn(Optional.of(posted));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.post("r-1"));
        assertEquals(ErrorCode.RECEIPT_ALREADY_POSTED.name(), ex.getReason());
    }

    @Test
    void post_draft_shouldEnsureRowLockAndAddOnHand() {
        InboundReceipt draft = InboundReceipt.builder().id("r-1").status(ReceiptStatus.DRAFT)
                .item(item()).quantity(1000).build();
        GoodsInventory inv = GoodsInventory.builder().item(item()).quantityOnHand(500).reservedQuantity(0).build();
        when(inboundReceiptRepository.findById("r-1")).thenReturn(Optional.of(draft));
        when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv));

        service.post("r-1");

        verify(goodsInventoryRepository).ensureRow("i-1");
        assertEquals(1500, inv.getQuantityOnHand());
        assertEquals(ReceiptStatus.POSTED, draft.getStatus());
    }
}
