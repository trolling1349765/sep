package fpt.capstone.service.impl;

import fpt.capstone.dto.response.AttachmentResponse;
import fpt.capstone.entity.ResourceAttachment;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.ResourceAttachmentRepository;
import fpt.capstone.service.FileStorageService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAttachmentServiceImplTest {

    @Mock
    private ResourceAttachmentRepository resourceAttachmentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ResourceAttachmentServiceImpl service;

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", ("data-" + name).getBytes());
    }

    @Nested
    @DisplayName("saveAll")
    class SaveAll {

        @BeforeEach
        void stubActor() {
            when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        }

        @Test
        void saveAll_shouldValidateStoreAndPersistEachFile() {
            when(fileStorageService.isValidFile(any())).thenReturn(true);
            when(fileStorageService.storeFile(any())).thenReturn("stored-a.pdf", "stored-b.pdf");
            when(resourceAttachmentRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0, ResourceAttachment.class));

            List<MultipartFile> files = List.of(pdf("a.pdf"), pdf("b.pdf"));
            List<AttachmentResponse> result =
                    service.saveAll(Table.FUND_USAGE_PLAN, "plan-1", AttachmentKind.LIST, files);

            assertEquals(2, result.size());
            verify(fileStorageService, org.mockito.Mockito.times(2)).storeFile(any());
            verify(resourceAttachmentRepository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.argThat(
                    a -> a.getOwnerType() == Table.FUND_USAGE_PLAN
                            && "plan-1".equals(a.getOwnerId())
                            && a.getKind() == AttachmentKind.LIST
                            && "user-1".equals(a.getUploadedBy())));
            assertTrue(result.stream().allMatch(r -> r.getKind() == AttachmentKind.LIST));
        }

        @Test
        void saveAll_invalidFile_shouldThrowArgumentInvalid() {
            when(fileStorageService.isValidFile(any())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.saveAll(Table.DONATION, "1", AttachmentKind.EVIDENCE, List.of(pdf("bad.exe"))));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verify(fileStorageService, never()).storeFile(any());
            verify(resourceAttachmentRepository, never()).save(any());
        }
    }

    @Test
    void saveAll_noFiles_shouldReturnEmptyWithoutTouchingCollaborators() {
        assertTrue(service.saveAll(Table.DONATION, "1", AttachmentKind.EVIDENCE, null).isEmpty());
        assertTrue(service.saveAll(Table.DONATION, "1", AttachmentKind.EVIDENCE, List.of()).isEmpty());
        verifyNoInteractions(fileStorageService, resourceAttachmentRepository, securityUtil);
    }

    @Test
    void list_shouldMapEntitiesToDto() {
        ResourceAttachment a = ResourceAttachment.builder()
                .id("att-1").ownerType(Table.DONATION).ownerId("1")
                .kind(AttachmentKind.EVIDENCE).fileName("e.pdf").fileUrl("stored.pdf").build();
        when(resourceAttachmentRepository.findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(Table.DONATION, "1"))
                .thenReturn(List.of(a));

        List<AttachmentResponse> result = service.list(Table.DONATION, "1");

        assertEquals(1, result.size());
        assertEquals("att-1", result.get(0).getId());
        assertEquals("e.pdf", result.get(0).getFileName());
    }

    @Test
    void listByKind_shouldDelegateToKindQuery() {
        when(resourceAttachmentRepository
                .findByOwnerTypeAndOwnerIdAndKindOrderByUploadedAtDesc(Table.FUND_USAGE_PLAN, "p1", AttachmentKind.COMPLETION))
                .thenReturn(List.of());

        assertTrue(service.listByKind(Table.FUND_USAGE_PLAN, "p1", AttachmentKind.COMPLETION).isEmpty());
        verify(resourceAttachmentRepository)
                .findByOwnerTypeAndOwnerIdAndKindOrderByUploadedAtDesc(Table.FUND_USAGE_PLAN, "p1", AttachmentKind.COMPLETION);
    }
}
