package fpt.capstone.service.impl;

import fpt.capstone.dto.request.DistributionCreateRequest;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.Distribution;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.DistributionStatus;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.PlanLineStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.DistributionRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.ItemAllocationPlanLineRepository;
import fpt.capstone.repository.ItemAllocationPlanRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributionServiceImplTest {

    @Mock private DistributionRepository distributionRepository;
    @Mock private ItemAllocationPlanLineRepository lineRepository;
    @Mock private ItemAllocationPlanRepository planRepository;
    @Mock private GoodsInventoryRepository goodsInventoryRepository;
    @Mock private ResourceAttachmentService resourceAttachmentService;
    @Mock private CodeGenerator codeGenerator;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private DistributionServiceImpl service;

    private SupportItem item() {
        return SupportItem.builder().id("i-1").name("Gao").build();
    }

    private ItemAllocationPlan plan(PlanStatus status) {
        return ItemAllocationPlan.builder().id("plan-1").code("KHPP-2026-001").item(item()).status(status).build();
    }

    private ItemAllocationPlanLine line(PlanStatus planStatus, PlanLineStatus lineStatus, int plannedQty) {
        return ItemAllocationPlanLine.builder()
                .id("line-1").plan(plan(planStatus)).beneficiary(Benificiary.builder().fullName("A").build())
                .plannedQty(plannedQty).issuedQty(0).lineStatus(lineStatus).build();
    }

    private DistributionCreateRequest req(int qty) {
        return DistributionCreateRequest.builder()
                .planLineId("line-1").actualQty(qty).issueDate(LocalDate.now()).recipientName("Pham An").build();
    }

    private MultipartFile file() {
        return new MockMultipartFile("files", "sign.pdf", "application/pdf", "x".getBytes());
    }

    @BeforeEach
    void stubs() {
        lenient().when(distributionRepository.save(any())).thenAnswer(i -> {
            Distribution d = i.getArgument(0, Distribution.class);
            d.setId("cp-1");
            return d;
        });
        lenient().when(lineRepository.save(any())).thenAnswer(i -> i.getArgument(0, ItemAllocationPlanLine.class));
        lenient().when(goodsInventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0, GoodsInventory.class));
        lenient().when(codeGenerator.next("CP")).thenReturn("CP-2026-001");
        lenient().when(securityUtil.getCurrentUserId()).thenReturn("officer-1");
    }

    @Test
    void create_lineNotFound_shouldThrow10054() {
        when(lineRepository.lockById("line-1")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(10), List.of(file())));
        assertEquals(ErrorCode.PLAN_LINE_NOT_FOUND.name(), ex.getReason());
    }

    @Test
    void create_planNotApproved_shouldThrow10043() {
        when(lineRepository.lockById("line-1"))
                .thenReturn(Optional.of(line(PlanStatus.PENDING_APPROVAL, PlanLineStatus.PENDING, 10)));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(10), List.of(file())));
        assertEquals(ErrorCode.PLAN_INVALID_STATE.name(), ex.getReason());
    }

    @Test
    void create_lineAlreadyIssued_shouldThrow10056() {
        when(lineRepository.lockById("line-1"))
                .thenReturn(Optional.of(line(PlanStatus.APPROVED, PlanLineStatus.ISSUED, 10)));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(10), List.of(file())));
        assertEquals(ErrorCode.LINE_ALREADY_ISSUED.name(), ex.getReason());
    }

    @Test
    void create_actualExceedsPlanned_shouldThrow10053() {
        when(lineRepository.lockById("line-1"))
                .thenReturn(Optional.of(line(PlanStatus.APPROVED, PlanLineStatus.PENDING, 10)));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(15), List.of(file())));
        assertEquals(ErrorCode.QUANTITY_EXCEEDS_RESERVED.name(), ex.getReason());
    }

    @Test
    void create_noFiles_shouldThrowEvidenceRequired() {
        when(lineRepository.lockById("line-1"))
                .thenReturn(Optional.of(line(PlanStatus.APPROVED, PlanLineStatus.PENDING, 10)));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(req(10), List.of()));
        assertEquals(ErrorCode.EVIDENCE_REQUIRED.name(), ex.getReason());
    }

    @Test
    void create_valid_shouldDrawBothOnHandAndReserved_andCompleteWhenLastLine() {
        ItemAllocationPlanLine line = line(PlanStatus.APPROVED, PlanLineStatus.PENDING, 10);
        GoodsInventory inv = GoodsInventory.builder().item(item()).quantityOnHand(1000).reservedQuantity(20).build();
        when(lineRepository.lockById("line-1")).thenReturn(Optional.of(line));
        when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv));
        when(lineRepository.countByPlanIdAndLineStatus("plan-1", PlanLineStatus.PENDING)).thenReturn(0L);

        DistributionResponse res = service.create(req(10), List.of(file()));

        assertEquals(990, inv.getQuantityOnHand());
        assertEquals(10, inv.getReservedQuantity()); // available unchanged (both dropped by 10)
        assertEquals(PlanLineStatus.ISSUED, line.getLineStatus());
        assertEquals(10, line.getIssuedQty());
        assertEquals("CP-2026-001", res.getCode());
        assertEquals(PlanStatus.COMPLETED, line.getPlan().getStatus());
        verify(resourceAttachmentService).saveAll(eq(Table.DISTRIBUTION), any(), any(), anyList());
    }

    @Test
    void confirm_issued_shouldBecomeRecipientConfirmed() {
        Distribution d = Distribution.builder().id("cp-1").status(DistributionStatus.ISSUED)
                .planLine(line(PlanStatus.COMPLETED, PlanLineStatus.ISSUED, 10)).build();
        when(distributionRepository.findById("cp-1")).thenReturn(Optional.of(d));

        service.confirm("cp-1");

        assertEquals(DistributionStatus.RECIPIENT_CONFIRMED, d.getStatus());
    }

    @Test
    void confirm_notIssued_shouldThrow10043() {
        Distribution d = Distribution.builder().id("cp-1").status(DistributionStatus.RECIPIENT_CONFIRMED).build();
        when(distributionRepository.findById("cp-1")).thenReturn(Optional.of(d));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.confirm("cp-1"));
        assertEquals(ErrorCode.PLAN_INVALID_STATE.name(), ex.getReason());
    }
}
