package fpt.capstone.service.impl;

import fpt.capstone.dto.request.CreateRightRequest;
import fpt.capstone.dto.request.UpdateRightRequest;
import fpt.capstone.dto.response.RightModuleResponse;
import fpt.capstone.dto.response.RightResponse;
import fpt.capstone.entity.Right;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.RightRepository;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RightServiceImplTest {

    @Mock
    private RightRepository rightRepository;
    @Mock
    private SystemLogService systemLogService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private RightServiceImpl rightService;

    private Right right(int id, String code, String module, String moduleName, int sortOrder) {
        Right r = Right.builder().code(code).module(module).moduleName(moduleName)
                .sortOrder(sortOrder).name(code).build();
        r.setId(id);
        return r;
    }

    @Nested
    @DisplayName("getCatalogue")
    class GetCatalogue {

        @Test
        void getCatalogue_shouldGroupRightsByModulePreservingOrder() {
            when(rightRepository.findAllByOrderByModuleAscSortOrderAsc()).thenReturn(List.of(
                    right(1, "REPORT_VIEW", "BAO_CAO", "Báo cáo", 1),
                    right(2, "REPORT_CREATE", "BAO_CAO", "Báo cáo", 2),
                    right(3, "POLICY_VIEW", "CHINH_SACH", "Chính sách", 1)));

            List<RightModuleResponse> catalogue = rightService.getCatalogue();

            assertEquals(2, catalogue.size());
            assertEquals("BAO_CAO", catalogue.get(0).getModule());
            assertEquals(2, catalogue.get(0).getRights().size());
            assertEquals("CHINH_SACH", catalogue.get(1).getModule());
        }
    }

    @Nested
    @DisplayName("createRight")
    class CreateRight {

        @ParameterizedTest
        @ValueSource(strings = {"lowercase", "REPORT-APPROVE", "_LEADING", "TRAILING_", "HAS SPACE", "1STARTS_WITH_DIGIT", ""})
        void createRight_shouldRejectInvalidCodes(String badCode) {
            CreateRightRequest request = CreateRightRequest.builder()
                    .code(badCode).name("x").module("BAO_CAO").build();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rightService.createRight(request));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.INVALID_RIGHT_CODE.name(), ex.getReason());
            verify(rightRepository, never()).save(any());
        }

        @Test
        void createRight_shouldRejectDuplicateCodeWith409() {
            when(rightRepository.existsByCode("REPORT_APPROVE")).thenReturn(true);
            CreateRightRequest request = CreateRightRequest.builder()
                    .code("REPORT_APPROVE").name("x").module("BAO_CAO").build();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rightService.createRight(request));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals(ErrorCode.RIGHT_CODE_EXISTS.name(), ex.getReason());
            verify(rightRepository, never()).save(any());
        }

        @Test
        void createRight_shouldForceIsSystemFalseAndReuseModuleName() {
            when(rightRepository.existsByCode("REPORT_APPROVE")).thenReturn(false);
            when(rightRepository.findAllByOrderByModuleAscSortOrderAsc()).thenReturn(List.of(
                    right(1, "REPORT_VIEW", "BAO_CAO", "Báo cáo", 1)));
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RightResponse response = rightService.createRight(CreateRightRequest.builder()
                    .code("REPORT_APPROVE").name("report Phê duyệt").module("BAO_CAO").build());

            assertFalse(response.isSystem());
            assertEquals("Báo cáo", response.getModuleName());
            assertEquals(2, response.getSortOrder());

            ArgumentCaptor<Right> saved = ArgumentCaptor.forClass(Right.class);
            verify(rightRepository).save(saved.capture());
            assertFalse(saved.getValue().isSystem());
            verify(systemLogService).write(argThat(log -> "RIGHT_CREATE".equals(log.getAction())));
        }

        @Test
        void createRight_shouldAcceptNewModuleWithProvidedModuleName() {
            when(rightRepository.existsByCode("EXPORT_MANAGE")).thenReturn(false);
            when(rightRepository.findAllByOrderByModuleAscSortOrderAsc()).thenReturn(List.of());
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RightResponse response = rightService.createRight(CreateRightRequest.builder()
                    .code("EXPORT_MANAGE").name("export Quản lý")
                    .module("XUAT_KHAU").moduleName("Xuất khẩu").build());

            assertEquals("Xuất khẩu", response.getModuleName());
            assertEquals(1, response.getSortOrder());
        }

        @Test
        void createRight_shouldRejectNullCode() {
            CreateRightRequest request = CreateRightRequest.builder()
                    .code(null).name("x").module("BAO_CAO").build();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rightService.createRight(request));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.INVALID_RIGHT_CODE.name(), ex.getReason());
            verify(rightRepository, never()).save(any());
        }

        @Test
        void createRight_shouldSwallowAuditLogFailure() {
            when(rightRepository.existsByCode("REPORT_APPROVE")).thenReturn(false);
            when(rightRepository.findAllByOrderByModuleAscSortOrderAsc()).thenReturn(List.of());
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            doThrow(new RuntimeException("log down")).when(systemLogService).write(any());

            RightResponse response = assertDoesNotThrow(() -> rightService.createRight(
                    CreateRightRequest.builder().code("REPORT_APPROVE").name("x").module("BAO_CAO").build()));

            assertEquals("REPORT_APPROVE", response.getCode());
        }
    }

    @Nested
    @DisplayName("updateRight")
    class UpdateRight {

        @Test
        void updateRight_shouldRejectUnknownRight() {
            when(rightRepository.findById(99)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rightService.updateRight(99, UpdateRightRequest.builder().name("x").build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.RIGHT_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void updateRight_shouldOnlyChangeNameAndDescription() {
            Right existing = right(1, "REPORT_VIEW", "BAO_CAO", "Báo cáo", 1);
            when(rightRepository.findById(1)).thenReturn(Optional.of(existing));
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RightResponse response = rightService.updateRight(1, UpdateRightRequest.builder()
                    .name("report Xem (mới)").description("Mô tả").build());

            assertEquals("report Xem (mới)", response.getName());
            assertEquals("Mô tả", response.getDescription());
            // Immutable identity fields survive the update
            assertEquals("REPORT_VIEW", response.getCode());
            assertFalse(response.isSystem());
            verify(systemLogService).write(argThat(log -> "RIGHT_UPDATE".equals(log.getAction())));
        }

        @Test
        void updateRight_shouldIgnoreNullNameAndNullDescription() {
            Right existing = right(1, "REPORT_VIEW", "BAO_CAO", "Báo cáo", 1);
            existing.setDescription("mô tả gốc");
            when(rightRepository.findById(1)).thenReturn(Optional.of(existing));
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RightResponse response = rightService.updateRight(1,
                    UpdateRightRequest.builder().name(null).description(null).build());

            // Both fields left untouched; only updateAt/By and log change
            assertEquals("REPORT_VIEW", response.getName());
            assertEquals("mô tả gốc", response.getDescription());
            verify(systemLogService).write(argThat(log -> "RIGHT_UPDATE".equals(log.getAction())));
        }

        @Test
        void updateRight_shouldIgnoreBlankNameButApplyDescription() {
            Right existing = right(1, "REPORT_VIEW", "BAO_CAO", "Báo cáo", 1);
            when(rightRepository.findById(1)).thenReturn(Optional.of(existing));
            when(rightRepository.save(any(Right.class))).thenAnswer(inv -> inv.getArgument(0));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RightResponse response = rightService.updateRight(1,
                    UpdateRightRequest.builder().name("   ").description("Mô tả mới").build());

            assertEquals("REPORT_VIEW", response.getName()); // blank name ignored
            assertEquals("Mô tả mới", response.getDescription());
        }
    }
}
