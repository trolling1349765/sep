package fpt.capstone.service.impl;

import fpt.capstone.dto.request.FundPlanRequest;
import fpt.capstone.dto.request.ReasonRequest;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.FundUsagePlan;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PlanStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BenificiaryRepository;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.FundUsagePlanRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
class FundUsagePlanServiceImplTest {

    @Mock private FundUsagePlanRepository planRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private BenificiaryRepository benificiaryRepository;
    @Mock private ResourceAttachmentService resourceAttachmentService;
    @Mock private CodeGenerator codeGenerator;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private FundUsagePlanServiceImpl service;

    private static final String CREATOR = "creator-1";
    private static final String APPROVER = "approver-2";

    private Donation confirmedDonation(String pending, String spent) {
        Donation d = Donation.builder().amount(new BigDecimal("100000000"))
                .pendingAmount(new BigDecimal(pending)).spentAmount(new BigDecimal(spent))
                .status(FundingStatus.CONFIRMED).build();
        d.setId(1);
        return d;
    }

    private FundPlanRequest req(String amount) {
        return FundPlanRequest.builder()
                .donationId(1).programName("Chuong trinh X")
                .amount(new BigDecimal(amount)).purpose("Mua xe lan")
                .expectedDate(LocalDate.now().plusDays(7)).build();
    }

    private FundUsagePlan plan(PlanStatus status, String amount) {
        FundUsagePlan p = FundUsagePlan.builder()
                .id("plan-1").code("KH-2026-001").donation(confirmedDonation("0", "0"))
                .programName("X").amount(new BigDecimal(amount)).status(status).build();
        p.setCreateBy(CREATOR);
        return p;
    }

    private MultipartFile file() {
        return new MockMultipartFile("files", "l.pdf", "application/pdf", "x".getBytes());
    }

    @BeforeEach
    void stubDetail() {
        lenient().when(resourceAttachmentService.listByKind(eq(Table.FUND_USAGE_PLAN), any(), any()))
                .thenReturn(List.of());
        lenient().when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, FundUsagePlan.class));
        lenient().when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Donation.class));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void create_bothTargets_shouldThrowArgumentInvalid() {
            FundPlanRequest r = req("10000000");
            r.setBeneficiaryId(5);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(r, List.of(file())));
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void create_donationNotConfirmed_shouldThrow10037() {
            Donation draft = confirmedDonation("0", "0");
            draft.setStatus(FundingStatus.DRAFT);
            when(donationRepository.findById(1)).thenReturn(Optional.of(draft));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req("10000000"), List.of(file())));
            assertEquals(ErrorCode.FUNDING_NOT_CONFIRMED.name(), ex.getReason());
        }

        @Test
        void create_amountExceedsAvailable_shouldThrow10041() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(confirmedDonation("90000000", "0")));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req("20000000"), List.of(file())));
            assertEquals(ErrorCode.FUNDING_BALANCE_INSUFFICIENT.name(), ex.getReason());
        }

        @Test
        void create_noFiles_shouldThrowEvidenceRequired() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(confirmedDonation("0", "0")));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req("20000000"), List.of()));
            assertEquals(ErrorCode.EVIDENCE_REQUIRED.name(), ex.getReason());
        }

        @Test
        void create_valid_shouldPersistPendingWithCodeAndList() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(confirmedDonation("0", "0")));
            when(codeGenerator.next("KH")).thenReturn("KH-2026-009");
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);

            service.create(req("60000000"), List.of(file()));

            org.mockito.ArgumentCaptor<FundUsagePlan> captor =
                    org.mockito.ArgumentCaptor.forClass(FundUsagePlan.class);
            verify(planRepository).save(captor.capture());
            assertEquals(PlanStatus.PENDING_APPROVAL, captor.getValue().getStatus());
            assertEquals("KH-2026-009", captor.getValue().getCode());
            verify(resourceAttachmentService).saveAll(eq(Table.FUND_USAGE_PLAN), any(),
                    eq(AttachmentKind.LIST), anyList());
            // create must not touch the donation balance
            verify(donationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        void approve_selfApproval_shouldThrow10042() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.PENDING_APPROVAL, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.approve("plan-1"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals(ErrorCode.SELF_APPROVAL_FORBIDDEN.name(), ex.getReason());
        }

        @Test
        void approve_notPending_shouldThrow10043() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.APPROVED, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn(APPROVER);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.approve("plan-1"));
            assertEquals(ErrorCode.PLAN_INVALID_STATE.name(), ex.getReason());
        }

        @Test
        void approve_insufficientAtApprovalTime_shouldThrow10041() {
            FundUsagePlan p = plan(PlanStatus.PENDING_APPROVAL, "60000000");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(APPROVER);
            // another plan already reserved 90M, leaving 10M
            when(donationRepository.findByIdForUpdate(1)).thenReturn(Optional.of(confirmedDonation("90000000", "0")));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.approve("plan-1"));
            assertEquals(ErrorCode.FUNDING_BALANCE_INSUFFICIENT.name(), ex.getReason());
            verify(donationRepository, never()).save(any());
        }

        @Test
        void approve_valid_shouldReservePending() {
            FundUsagePlan p = plan(PlanStatus.PENDING_APPROVAL, "60000000");
            Donation locked = confirmedDonation("0", "0");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(APPROVER);
            when(donationRepository.findByIdForUpdate(1)).thenReturn(Optional.of(locked));

            service.approve("plan-1");

            assertEquals(new BigDecimal("60000000"), locked.getPendingAmount());
            assertEquals(PlanStatus.APPROVED, p.getStatus());
            assertEquals(APPROVER, p.getApprovedBy());
        }
    }

    @Nested
    @DisplayName("reject / cancel / complete / delete")
    class OtherTransitions {

        @Test
        void reject_missingReason_shouldThrow10045() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.PENDING_APPROVAL, "60000000")));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.reject("plan-1", new ReasonRequest("  ")));
            assertEquals(ErrorCode.REASON_REQUIRED.name(), ex.getReason());
        }

        @Test
        void cancelApproved_shouldRollbackPending() {
            FundUsagePlan p = plan(PlanStatus.APPROVED, "60000000");
            Donation locked = confirmedDonation("60000000", "0");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            when(donationRepository.findByIdForUpdate(1)).thenReturn(Optional.of(locked));

            service.cancel("plan-1", new ReasonRequest("sai sot"));

            assertEquals(new BigDecimal("0"), locked.getPendingAmount());
            assertEquals(PlanStatus.CANCELLED, p.getStatus());
        }

        @Test
        void cancel_byNonCreator_shouldThrowAccessDenied() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.APPROVED, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn("someone-else");
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.cancel("plan-1", new ReasonRequest("x")));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals(ErrorCode.ACCESS_DENIED.name(), ex.getReason());
        }

        @Test
        void completeApproved_shouldMovePendingToSpent() {
            FundUsagePlan p = plan(PlanStatus.APPROVED, "60000000");
            Donation locked = confirmedDonation("60000000", "0");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            when(donationRepository.findByIdForUpdate(1)).thenReturn(Optional.of(locked));

            service.complete("plan-1", List.of(file()));

            assertEquals(new BigDecimal("0"), locked.getPendingAmount());
            assertEquals(new BigDecimal("60000000"), locked.getSpentAmount());
            assertEquals(PlanStatus.COMPLETED, p.getStatus());
            verify(resourceAttachmentService).saveAll(eq(Table.FUND_USAGE_PLAN), any(),
                    eq(AttachmentKind.COMPLETION), anyList());
        }

        @Test
        void complete_noFiles_shouldThrowEvidenceRequired() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.APPROVED, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.complete("plan-1", List.of()));
            assertEquals(ErrorCode.EVIDENCE_REQUIRED.name(), ex.getReason());
        }

        @Test
        void deleteApproved_shouldThrow10043() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.APPROVED, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.delete("plan-1", new ReasonRequest("x")));
            assertEquals(ErrorCode.PLAN_INVALID_STATE.name(), ex.getReason());
        }

        @Test
        void deletePending_shouldSoftDelete() {
            FundUsagePlan p = plan(PlanStatus.PENDING_APPROVAL, "60000000");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);

            service.delete("plan-1", new ReasonRequest("nhap sai"));

            assertEquals(PlanStatus.DELETED, p.getStatus());
            assertEquals("nhap sai", p.getDeleteReason());
        }
    }

    @Nested
    @DisplayName("update rejected -> resubmit")
    class Update {

        @Test
        void update_rejected_shouldResubmitAsPending() {
            FundUsagePlan p = plan(PlanStatus.REJECTED, "60000000");
            p.setRejectReason("thieu ho so");
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(p));
            when(securityUtil.getCurrentUserId()).thenReturn(CREATOR);
            when(donationRepository.findById(1)).thenReturn(Optional.of(confirmedDonation("0", "0")));

            service.update("plan-1", req("50000000"), null);

            assertEquals(PlanStatus.PENDING_APPROVAL, p.getStatus());
            assertEquals(null, p.getRejectReason());
        }

        @Test
        void update_byNonCreator_shouldThrowAccessDenied() {
            when(planRepository.findById("plan-1")).thenReturn(Optional.of(plan(PlanStatus.PENDING_APPROVAL, "60000000")));
            when(securityUtil.getCurrentUserId()).thenReturn("intruder");
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.update("plan-1", req("50000000"), null));
            assertEquals(ErrorCode.ACCESS_DENIED.name(), ex.getReason());
        }
    }
}
