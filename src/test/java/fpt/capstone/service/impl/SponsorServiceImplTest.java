package fpt.capstone.service.impl;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.request.UpdateSponsorStatusRequest;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SponsorDetailResponse;
import fpt.capstone.dto.response.SponsorListResponse;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.SponsorRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SponsorServiceImplTest {

    @Mock
    private SponsorRepository sponsorRepository;
    @Mock
    private DonationRepository donationRepository;
    @Mock
    private fpt.capstone.repository.InboundReceiptRepository inboundReceiptRepository;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private SponsorServiceImpl service;

    private SponsorRequest validRequest(String name) {
        return SponsorRequest.builder()
                .name(name)
                .type(SponsorType.ORG)
                .orgCode("ORG-1")
                .contactPerson("Nguyen Van A")
                .phone("0912345678")
                .email("a@example.com")
                .address("Ha Noi")
                .note("note")
                .build();
    }

    private Sponsor existing(String id, SponsorStatus status) {
        return Sponsor.builder()
                .id(id).code("NTT-2026-001").name("Old").normalizedName("old")
                .type(SponsorType.ORG).phone("0900000000").status(status).build();
    }

    @BeforeEach
    void stubReceiptHistory() {
        lenient().when(inboundReceiptRepository.findBySponsorIdAndIsDeleteFalse(any()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @BeforeEach
        void actor() {
            lenient().when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        }

        @Test
        void create_noDuplicate_shouldGenerateCodeAndPersistActive() {
            when(sponsorRepository.findDuplicates(any(), anyString(), any(), isNull()))
                    .thenReturn(List.of());
            when(codeGenerator.next("NTT")).thenReturn("NTT-2026-007");
            when(sponsorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Sponsor.class));

            service.create(validRequest("Quỹ Từ Thiện ABC"));

            ArgumentCaptor<Sponsor> captor = ArgumentCaptor.forClass(Sponsor.class);
            verify(sponsorRepository).save(captor.capture());
            Sponsor saved = captor.getValue();
            assertEquals("NTT-2026-007", saved.getCode());
            assertEquals(SponsorStatus.ACTIVE, saved.getStatus());
            assertEquals("user-1", saved.getCreateBy());
            // accents stripped, đ->d, lowercased, whitespace collapsed
            assertEquals("quy tu thien abc", saved.getNormalizedName());
        }

        @Test
        void create_duplicatePhone_shouldThrowInvalidArgsWithDuplicateData() {
            Sponsor dup = existing("dup-1", SponsorStatus.ACTIVE);
            when(sponsorRepository.findDuplicates(any(), anyString(), any(), isNull()))
                    .thenReturn(List.of(dup));

            InvalidArgsException ex = assertThrows(InvalidArgsException.class,
                    () -> service.create(validRequest("Anything")));

            assertEquals(ErrorCode.SPONSOR_DUPLICATED.getCode(), ex.getResponse().getCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) ex.getResponse().getData();
            assertEquals("dup-1", data.get("duplicateId"));
            assertEquals("NTT-2026-001", data.get("duplicateCode"));
            verify(sponsorRepository, never()).save(any());
        }

        @Test
        void create_blankOrgCode_shouldPassNullOrgCodeToDuplicateCheck() {
            SponsorRequest req = validRequest("Name");
            req.setOrgCode("   ");
            when(sponsorRepository.findDuplicates(isNull(), anyString(), any(), isNull()))
                    .thenReturn(List.of());
            when(codeGenerator.next("NTT")).thenReturn("NTT-2026-001");
            when(sponsorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Sponsor.class));

            service.create(req);

            verify(sponsorRepository).findDuplicates(isNull(), eq("0912345678"), anyString(), isNull());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void update_notFound_shouldThrow404() {
            when(sponsorRepository.findById("missing")).thenReturn(java.util.Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.update("missing", validRequest("X")));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.SPONSOR_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void update_duplicateOtherSponsor_shouldThrow10035ExcludingSelf() {
            Sponsor self = existing("s-1", SponsorStatus.ACTIVE);
            when(sponsorRepository.findById("s-1")).thenReturn(java.util.Optional.of(self));
            when(sponsorRepository.findDuplicates(any(), anyString(), any(), eq("s-1")))
                    .thenReturn(List.of(existing("other", SponsorStatus.ACTIVE)));

            assertThrows(InvalidArgsException.class, () -> service.update("s-1", validRequest("New")));
            verify(sponsorRepository, never()).save(any());
        }

        @Test
        void update_valid_shouldMutateFields() {
            Sponsor self = existing("s-1", SponsorStatus.ACTIVE);
            when(sponsorRepository.findById("s-1")).thenReturn(java.util.Optional.of(self));
            when(sponsorRepository.findDuplicates(any(), anyString(), any(), eq("s-1")))
                    .thenReturn(List.of());
            when(securityUtil.getCurrentUserId()).thenReturn("user-9");
            when(sponsorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Sponsor.class));
            when(donationRepository.findBySponsorId("s-1")).thenReturn(List.of());

            SponsorDetailResponse res = service.update("s-1", validRequest("Đổi Tên"));

            assertEquals("Đổi Tên", res.getName());
            assertEquals("doi ten", self.getNormalizedName());
            assertEquals("user-9", self.getUpdateBy());
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        void changeStatus_invalidValue_shouldThrowInvalidStatus() {
            when(sponsorRepository.findById("s-1"))
                    .thenReturn(java.util.Optional.of(existing("s-1", SponsorStatus.ACTIVE)));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.changeStatus("s-1", new UpdateSponsorStatusRequest("BANNED")));

            assertEquals(ErrorCode.INVALID_STATUS.name(), ex.getReason());
        }

        @Test
        void changeStatus_sameStatus_shouldThrowInvalidStatus() {
            when(sponsorRepository.findById("s-1"))
                    .thenReturn(java.util.Optional.of(existing("s-1", SponsorStatus.ACTIVE)));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.changeStatus("s-1", new UpdateSponsorStatusRequest("ACTIVE")));

            assertEquals(ErrorCode.INVALID_STATUS.name(), ex.getReason());
        }

        @Test
        void changeStatus_toggle_shouldPersistNewStatus() {
            Sponsor self = existing("s-1", SponsorStatus.ACTIVE);
            when(sponsorRepository.findById("s-1")).thenReturn(java.util.Optional.of(self));
            when(securityUtil.getCurrentUserId()).thenReturn("user-1");
            when(sponsorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Sponsor.class));
            when(donationRepository.findBySponsorId("s-1")).thenReturn(List.of());

            service.changeStatus("s-1", new UpdateSponsorStatusRequest("inactive"));

            assertEquals(SponsorStatus.INACTIVE, self.getStatus());
        }
    }

    @Nested
    @DisplayName("getById / history")
    class GetById {

        @Test
        void getById_notFound_shouldThrow404() {
            when(sponsorRepository.findById("x")).thenReturn(java.util.Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getById("x"));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void getById_shouldBuildFundingHistorySortedDesc() {
            Sponsor self = existing("s-1", SponsorStatus.ACTIVE);
            when(sponsorRepository.findById("s-1")).thenReturn(java.util.Optional.of(self));
            Donation older = Donation.builder().code("KP-2026-001").name("Old")
                    .amount(new BigDecimal("100")).status(FundingStatus.CONFIRMED)
                    .transferDate(LocalDate.of(2026, 1, 1)).build();
            Donation newer = Donation.builder().code("KP-2026-002").name("New")
                    .amount(new BigDecimal("200")).status(FundingStatus.DRAFT)
                    .transferDate(LocalDate.of(2026, 6, 1)).build();
            when(donationRepository.findBySponsorId("s-1")).thenReturn(List.of(older, newer));

            SponsorDetailResponse res = service.getById("s-1");

            assertEquals(2, res.getContributionHistory().size());
            assertEquals("KP-2026-002", res.getContributionHistory().get(0).getCode());
        }
    }

    @Nested
    @DisplayName("search paging guards")
    class Search {

        @Test
        void search_negativePage_shouldThrowArgumentInvalid() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.search(-1, 20, null, null, null, "createdAt", "desc"));
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void search_badSortKey_shouldThrowArgumentInvalid() {
            assertThrows(ResponseStatusException.class,
                    () -> service.search(0, 20, null, null, null, "password", "desc"));
        }

        @Test
        void search_badDir_shouldThrowArgumentInvalid() {
            assertThrows(ResponseStatusException.class,
                    () -> service.search(0, 20, null, null, null, "createdAt", "sideways"));
        }

        @Test
        void search_valid_shouldReturnMappedPage() {
            when(sponsorRepository.search(isNull(), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(existing("s-1", SponsorStatus.ACTIVE))));

            PageResponse<SponsorListResponse> page =
                    service.search(0, 20, "  ", null, null, "createdAt", "desc");

            assertEquals(1, page.getItems().size());
            assertTrue(page.getItems().get(0).getCode().startsWith("NTT-"));
        }
    }
}
