package fpt.capstone.service.impl;

import fpt.capstone.dto.request.ItemPlanLineRequest;
import fpt.capstone.dto.request.ItemPlanRequest;
import fpt.capstone.dto.request.NotDeliveredRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.NotificationType;
import fpt.capstone.enums.PlanLineStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.ItemAllocationPlanLineRepository;
import fpt.capstone.repository.ItemAllocationPlanRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.NotificationService;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemAllocationPlanServiceImplTest {

    @Mock private ItemAllocationPlanRepository planRepository;
    @Mock private ItemAllocationPlanLineRepository lineRepository;
    @Mock private SupportItemRepository supportItemRepository;
    @Mock private BenificiaryRepository benificiaryRepository;
    @Mock private GoodsInventoryRepository goodsInventoryRepository;
    @Mock private ResourceAttachmentService resourceAttachmentService;
    @Mock private NotificationService notificationService;
    @Mock private CodeGenerator codeGenerator;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private ItemAllocationPlanServiceImpl service;

    private static final String CREATOR = "creator-1";
    private static final String APPROVER = "approver-2";

    private SupportItem item() {
        return SupportItem.builder().id("i-1").code("VP-2026-001").name("Gao").unit("kg").build();
    }

    private GoodsInventory inv(int onHand, int reserved) {
        return GoodsInventory.builder().item(item()).quantityOnHand(onHand).reservedQuantity(reserved).build();
    }

    private ItemAllocationPlan plan(PlanStatus status, int plannedQty) {
        ItemAllocationPlan p = ItemAllocationPlan.builder()
                .id("plan-1").code("KHPP-2026-001").item(item()).plannedQty(plannedQty).status(status)
                .deliveryPlace("UBND xa").expectedDate(LocalDate.of(2026, 12, 31)).build();
        p.setCreateBy(CREATOR);
        return p;
    }

    private ItemPlanRequest req(int total, int q1, int q2) {
        return ItemPlanRequest.builder()
                .itemId("i-1").plannedQty(total).expectedDate(LocalDate.of(2026, 12, 31)).deliveryPlace("UBND")
                .lines(List.of(
                        ItemPlanLineRequest.builder().beneficiaryId(1).plannedQty(q1).build(),
                        ItemPlanLineRequest.builder().beneficiaryId(2).plannedQty(q2).build()))
                .build();
    }

    private MultipartFile file() {
        return new MockMultipartFile("files", "l.pdf", "application/pdf", "x".getBytes());
    }

    @BeforeEach
    void stubDetail() {
        lenient().when(lineRepository.findByPlanId(any())).thenReturn(List.of());
        lenient().when(resourceAttachmentService.listByKind(any(), any(), any())).thenReturn(List.of());
        lenient().when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0, ItemAllocationPlan.class));
        lenient().when(lineRepository.save(any())).thenAnswer(i -> i.getArgument(0, ItemAllocationPlanLine.class));
        lenient().when(goodsInventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0, GoodsInventory.class));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void create_lineSumMismatch_shouldThrow10052() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req(30, 10, 10), List.of(file())));
            assertEquals(ErrorCode.LINE_SUM_MISMATCH.name(), ex.getReason());
        }

        @Test
        void create_duplicateBeneficiary_shouldThrowArgumentInvalid() {
            ItemPlanRequest r = ItemPlanRequest.builder()
                    .itemId("i-1").plannedQty(20).expectedDate(LocalDate.of(2026, 12, 31)).deliveryPlace("x")
                    .lines(List.of(
                            ItemPlanLineRequest.builder().beneficiaryId(1).plannedQty(10).build(),
                            ItemPlanLineRequest.builder().beneficiaryId(1).plannedQty(10).build()))
                    .build();
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(r, List.of(file())));
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void create_overAvailable_shouldThrow10047() {
            when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
            when(goodsInventoryRepository.findByItem_Id("i-1")).thenReturn(Optional.of(inv(15, 0)));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req(20, 10, 10), List.of(file())));
            assertEquals(ErrorCode.STOCK_INSUFFICIENT.name(), ex.getReason());
        }

        @Test
        void create_valid_shouldPersistPendingWithoutReserving() {
            when(supportItemRepository.findById("i-1")).thenReturn(Optional.of(item()));
            when(goodsInventoryRepository.findByItem_Id("i-1")).thenReturn(Optional.of(inv(1000, 0)));
            when(codeGenerator.next("KHPP")).thenReturn("KHPP-2026-005");
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            when(benificiaryRepository.findById(any()))
                    .thenReturn(Optional.of(Benificiary.builder().fullName("A").build()));

            service.create(req(20, 10, 10), List.of(file()));

            verify(goodsInventoryRepository, never()).save(any());
            verify(lineRepository, org.mockito.Mockito.times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("approve + notification")
    class Approve {

        @Test
        void approve_self_shouldThrow10042() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.PENDING_APPROVAL, 20)));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.approve("plan-1"));
            assertEquals(ErrorCode.SELF_APPROVAL_FORBIDDEN.name(), ex.getReason());
        }

        @Test
        void approve_overAvailable_shouldThrow10047() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.PENDING_APPROVAL, 20)));
            when(securityUtil.getCurrentUserId()).thenReturn(APPROVER);
            when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inv(15, 0)));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.approve("plan-1"));
            assertEquals(ErrorCode.STOCK_INSUFFICIENT.name(), ex.getReason());
            verify(goodsInventoryRepository, never()).save(any());
        }

        @Test
        void approve_valid_shouldReserveAndNotifyLinkedBeneficiary() {
            ItemAllocationPlan p = plan(PlanStatus.PENDING_APPROVAL, 20);
            GoodsInventory inventory = inv(1000, 0);
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(APPROVER);
            when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inventory));
            // one line linked to a user, one not
            User user = User.builder().id("u-1").build();
            Benificiary linked = Benificiary.builder().fullName("A")
                    .application(Application.builder().submitBy(user).build()).build();
            Benificiary unlinked = Benificiary.builder().fullName("B").build();
            when(lineRepository.findByPlanId("plan-1")).thenReturn(List.of(
                    ItemAllocationPlanLine.builder().plannedQty(10).beneficiary(linked).build(),
                    ItemAllocationPlanLine.builder().plannedQty(10).beneficiary(unlinked).build()));

            service.approve("plan-1");

            assertEquals(20, inventory.getReservedQuantity());
            assertEquals(PlanStatus.APPROVED, p.getStatus());
            // exactly one notification (the linked beneficiary); no active tx so dispatch runs inline
            verify(notificationService).sendNotification(eq(user), eq(NotificationType.APPROVAL),
                    any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("cancel / not-delivered / delete")
    class Others {

        @Test
        void cancelApproved_shouldReleaseRemainingReserve() {
            ItemAllocationPlan p = plan(PlanStatus.APPROVED, 20);
            GoodsInventory inventory = inv(1000, 20);
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            when(lineRepository.findByPlanId("plan-1")).thenReturn(List.of(
                    ItemAllocationPlanLine.builder().plannedQty(10).issuedQty(0).lineStatus(PlanLineStatus.PENDING).build(),
                    ItemAllocationPlanLine.builder().plannedQty(10).issuedQty(6).lineStatus(PlanLineStatus.ISSUED).build()));
            when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inventory));

            service.cancel("plan-1", new ReasonRequest("khong can"));

            // released = (10-0) + (10-6) = 14 -> reserved 20 - 14 = 6
            assertEquals(6, inventory.getReservedQuantity());
            assertEquals(PlanStatus.CANCELLED, p.getStatus());
        }

        @Test
        void notDelivered_returnStock_shouldReleaseAndMaybeComplete() {
            ItemAllocationPlan p = plan(PlanStatus.APPROVED, 10);
            GoodsInventory inventory = inv(1000, 10);
            ItemAllocationPlanLine line = ItemAllocationPlanLine.builder()
                    .id("line-1").plan(p).plannedQty(10).issuedQty(0).lineStatus(PlanLineStatus.PENDING).build();
            when(lineRepository.lockById("line-1")).thenReturn(Optional.of(line));
            when(goodsInventoryRepository.lockByItemId("i-1")).thenReturn(Optional.of(inventory));
            when(lineRepository.countByPlanIdAndLineStatus("plan-1", PlanLineStatus.PENDING)).thenReturn(0L);

            service.markNotDelivered("line-1", new NotDeliveredRequest("vang mat", true));

            assertEquals(0, inventory.getReservedQuantity());
            assertEquals(PlanLineStatus.NOT_DELIVERED, line.getLineStatus());
            assertEquals(PlanStatus.COMPLETED, p.getStatus());
        }

        @Test
        void notDelivered_alreadyIssued_shouldThrow10056() {
            ItemAllocationPlan p = plan(PlanStatus.APPROVED, 10);
            ItemAllocationPlanLine line = ItemAllocationPlanLine.builder()
                    .id("line-1").plan(p).plannedQty(10).lineStatus(PlanLineStatus.ISSUED).build();
            when(lineRepository.lockById("line-1")).thenReturn(Optional.of(line));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.markNotDelivered("line-1", new NotDeliveredRequest("x", true)));
            assertEquals(ErrorCode.LINE_ALREADY_ISSUED.name(), ex.getReason());
        }

        @Test
        void deleteApproved_shouldThrow10043() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.APPROVED, 20)));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.delete("plan-1", new ReasonRequest("x")));
            assertEquals(ErrorCode.PLAN_INVALID_STATE.name(), ex.getReason());
        }
    }
}
