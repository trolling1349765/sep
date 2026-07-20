package fpt.capstone.service.impl;

import fpt.capstone.dto.request.FundingCreateRequest;
import fpt.capstone.dto.response.FundingDetailResponse;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PaymentMethod;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.ResourceAttachmentService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FundingServiceImplTest {

    @Mock
    private DonationRepository donationRepository;
    @Mock
    private SponsorRepository sponsorRepository;
    @Mock
    private fpt.capstone.repository.FundUsagePlanRepository fundUsagePlanRepository;
    @Mock
    private ResourceAttachmentService resourceAttachmentService;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private FundingServiceImpl service;

    private FundingCreateRequest req(PaymentMethod method, String ref) {
        return FundingCreateRequest.builder()
                .name("Quy X")
                .amount(new BigDecimal("100000000"))
                .receivedDate(LocalDate.now())
                .purpose("Ho tro")
                .paymentMethod(method)
                .transactionRef(ref)
                .evidenceName("Bien ban")
                .build();
    }

    private MultipartFile pdf() {
        return new MockMultipartFile("files", "e.pdf", "application/pdf", "x".getBytes());
    }

    private Donation persisted(int id, FundingStatus status) {
        Donation d = Donation.builder().name("Quy X").amount(new BigDecimal("100000000"))
                .pendingAmount(BigDecimal.ZERO).spentAmount(BigDecimal.ZERO)
                .status(status).paymentMethod(PaymentMethod.CASH).transferDate(LocalDate.now()).build();
        d.setId(id);
        return d;
    }

    @BeforeEach
    void stubToDetail() {
        lenient().when(resourceAttachmentService.list(eq(Table.DONATION), any())).thenReturn(List.of());
        lenient().when(fundUsagePlanRepository.findByDonationId(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
        lenient().when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        lenient().when(codeGenerator.next("KP")).thenReturn("KP-2026-001");
        lenient().when(donationRepository.save(any())).thenAnswer(inv -> {
            Donation d = inv.getArgument(0, Donation.class);
            if (d.getId() == 0) {
                d.setId(42);
            }
            return d;
        });
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void create_cashNoFiles_shouldBeDraftWithoutAttachments() {
            FundingDetailResponse res = service.create(req(PaymentMethod.CASH, null), null);

            ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
            verify(donationRepository).save(captor.capture());
            assertEquals(FundingStatus.DRAFT, captor.getValue().getStatus());
            assertEquals("KP-2026-001", captor.getValue().getCode());
            assertEquals(BigDecimal.ZERO, captor.getValue().getPendingAmount());
            assertEquals("user-1", captor.getValue().getRecordedBy());
            verify(resourceAttachmentService, never()).saveAll(any(), any(), any(), anyList());
            assertEquals(FundingStatus.DRAFT, res.getStatus());
        }

        @Test
        void create_withFiles_shouldBeConfirmedAndStoreEvidence() {
            service.create(req(PaymentMethod.CASH, null), List.of(pdf()));

            ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
            verify(donationRepository).save(captor.capture());
            assertEquals(FundingStatus.CONFIRMED, captor.getValue().getStatus());
            verify(resourceAttachmentService).saveAll(eq(Table.DONATION), eq("42"),
                    eq(AttachmentKind.EVIDENCE), anyList());
        }

        @Test
        void create_transferWithoutRef_shouldThrow10039() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(req(PaymentMethod.TRANSFER, "  "), List.of(pdf())));
            assertEquals(ErrorCode.TRANSACTION_REF_REQUIRED.name(), ex.getReason());
            verify(donationRepository, never()).save(any());
        }

        @Test
        void create_transferWithRef_shouldSucceed() {
            service.create(req(PaymentMethod.TRANSFER, "TXN-1"), null);
            verify(donationRepository).save(any());
        }

        @Test
        void create_inactiveSponsor_shouldThrowSponsorNotFound() {
            Sponsor inactive = Sponsor.builder().id("s-1").status(SponsorStatus.INACTIVE).build();
            when(sponsorRepository.findById("s-1")).thenReturn(Optional.of(inactive));
            FundingCreateRequest r = req(PaymentMethod.CASH, null);
            r.setSponsorId("s-1");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.create(r, null));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.SPONSOR_NOT_FOUND.name(), ex.getReason());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void update_notFound_shouldThrow10036() {
            when(donationRepository.findById(9)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.update(9, req(PaymentMethod.CASH, null), null));
            assertEquals(ErrorCode.FUNDING_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void update_confirmed_shouldThrowFundingLocked() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(persisted(1, FundingStatus.CONFIRMED)));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.update(1, req(PaymentMethod.CASH, null), null));
            assertEquals(ErrorCode.FUNDING_LOCKED.name(), ex.getReason());
        }

        @Test
        void update_draft_shouldMutateAndAppendFiles() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(persisted(1, FundingStatus.DRAFT)));
            service.update(1, req(PaymentMethod.CASH, null), List.of(pdf()));
            verify(resourceAttachmentService).saveAll(eq(Table.DONATION), eq("1"),
                    eq(AttachmentKind.EVIDENCE), anyList());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        void confirm_alreadyConfirmed_shouldThrowFundingLocked() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(persisted(1, FundingStatus.CONFIRMED)));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.confirm(1));
            assertEquals(ErrorCode.FUNDING_LOCKED.name(), ex.getReason());
        }

        @Test
        void confirm_noEvidence_shouldThrowEvidenceRequired() {
            when(donationRepository.findById(1)).thenReturn(Optional.of(persisted(1, FundingStatus.DRAFT)));
            when(resourceAttachmentService.listByKind(Table.DONATION, "1", AttachmentKind.EVIDENCE))
                    .thenReturn(List.of());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.confirm(1));
            assertEquals(ErrorCode.EVIDENCE_REQUIRED.name(), ex.getReason());
        }

        @Test
        void confirm_withEvidence_shouldTransitionToConfirmed() {
            Donation draft = persisted(1, FundingStatus.DRAFT);
            when(donationRepository.findById(1)).thenReturn(Optional.of(draft));
            when(resourceAttachmentService.listByKind(Table.DONATION, "1", AttachmentKind.EVIDENCE))
                    .thenReturn(List.of(fpt.capstone.dto.response.AttachmentResponse.builder().id("a").build()));

            FundingDetailResponse res = service.confirm(1);

            assertEquals(FundingStatus.CONFIRMED, draft.getStatus());
            assertEquals(FundingStatus.CONFIRMED, res.getStatus());
        }
    }

    @Nested
    @DisplayName("search guards")
    class Search {

        @Test
        void search_badSort_shouldThrowArgumentInvalid() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.search(0, 20, null, null, null, null, "evil", "desc"));
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void search_negativePage_shouldThrowArgumentInvalid() {
            assertThrows(ResponseStatusException.class,
                    () -> service.search(-1, 20, null, null, null, null, "createdAt", "desc"));
        }
    }
}
