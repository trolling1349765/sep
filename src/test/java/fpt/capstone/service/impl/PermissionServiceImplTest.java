package fpt.capstone.service.impl;

import fpt.capstone.dto.response.RolePermissionsResponse;
import fpt.capstone.dto.response.RoleSummaryResponse;
import fpt.capstone.entity.Permission;
import fpt.capstone.entity.Right;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.PermissionRepository;
import fpt.capstone.repository.RightRepository;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.service.SystemLogService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RightRepository rightRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private SystemLogService systemLogService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Role citizenRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        citizenRole = Role.builder().name("Citizen").description("Công dân").build();
        citizenRole.setId(1);
        adminRole = Role.builder().name("Admin").description("Quản trị").build();
        adminRole.setId(8);
    }

    private Right right(int id, String code, boolean system) {
        Right r = Right.builder().code(code).isSystem(system).module("M").build();
        r.setId(id);
        return r;
    }

    @Nested
    @DisplayName("getRoles")
    class GetRoles {

        @Test
        void getRoles_shouldReturnRolesWithGrantedCounts() {
            when(roleRepository.findAll()).thenReturn(List.of(citizenRole, adminRole));
            PermissionRepository.RoleGrantCount count = mock(PermissionRepository.RoleGrantCount.class);
            when(count.getRoleId()).thenReturn(1);
            when(count.getGrantedCount()).thenReturn(19L);
            when(permissionRepository.countGrantsGroupedByRole()).thenReturn(List.of(count));

            List<RoleSummaryResponse> roles = permissionService.getRoles();

            assertEquals(2, roles.size());
            assertEquals(19L, roles.get(0).getGrantedCount());
            assertEquals(0L, roles.get(1).getGrantedCount());
        }
    }

    @Nested
    @DisplayName("getRolePermissions")
    class GetRolePermissions {

        @Test
        void getRolePermissions_shouldReturnSortedRightIds() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            when(permissionRepository.findRightIdsByRoleId(1)).thenReturn(Set.of(3, 1, 2));

            RolePermissionsResponse response = permissionService.getRolePermissions(1);

            assertEquals(List.of(1, 2, 3), response.getRightIds());
            assertEquals(3, response.getGrantedCount());
            assertEquals("Citizen", response.getRoleName());
        }

        @Test
        void getRolePermissions_shouldThrow404WhenRoleMissing() {
            when(roleRepository.findById(99)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> permissionService.getRolePermissions(99));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.ROLE_NOT_FOUND.name(), ex.getReason());
        }
    }

    @Nested
    @DisplayName("updateRolePermissions")
    class UpdateRolePermissions {

        @Test
        void updateRolePermissions_shouldApplyDiffWithoutDeleteAllReinsert() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            Right kept = right(1, "PROFILE_VIEW", true);
            Right added = right(3, "POLICY_VIEW", false);
            when(rightRepository.findAllById(Set.of(1, 3))).thenReturn(List.of(kept, added));
            when(permissionRepository.findRightIdsByRoleId(1)).thenReturn(Set.of(1, 2));
            when(rightRepository.findAllById(Set.of(2)))
                    .thenReturn(List.of(right(2, "CHATBOT_USE", false)));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RolePermissionsResponse response = permissionService.updateRolePermissions(1, List.of(1, 3));

            // Only the diff is written: right 2 removed, right 3 inserted, right 1 untouched
            verify(permissionRepository).deleteByRoleIdAndRightIdIn(eq(1), eq(Set.of(2)));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Permission>> inserted = ArgumentCaptor.forClass(List.class);
            verify(permissionRepository).saveAll(inserted.capture());
            assertEquals(1, inserted.getValue().size());
            assertEquals(3, inserted.getValue().get(0).getRight().getId());
            assertEquals(List.of(1, 3), response.getRightIds());
        }

        @Test
        void updateRolePermissions_shouldWritePermissionUpdateAuditLog() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            when(rightRepository.findAllById(Set.of(1))).thenReturn(List.of(right(1, "PROFILE_VIEW", true)));
            when(permissionRepository.findRightIdsByRoleId(1)).thenReturn(Set.of(1));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            permissionService.updateRolePermissions(1, List.of(1));

            ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
            verify(systemLogService).write(logCaptor.capture());
            assertEquals("PERMISSION_UPDATE", logCaptor.getValue().getAction());
            assertEquals("admin-1", logCaptor.getValue().getUserId());
        }

        @Test
        void updateRolePermissions_shouldRejectAdminRole() {
            when(roleRepository.findById(8)).thenReturn(adminRole);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> permissionService.updateRolePermissions(8, List.of(1)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals(ErrorCode.ADMIN_ROLE_LOCKED.name(), ex.getReason());
            verify(permissionRepository, never()).deleteByRoleIdAndRightIdIn(anyInt(), anyCollection());
            verify(permissionRepository, never()).saveAll(any());
        }

        @Test
        void updateRolePermissions_shouldRejectUnknownRightId() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            when(rightRepository.findAllById(Set.of(1, 999)))
                    .thenReturn(List.of(right(1, "PROFILE_VIEW", true)));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> permissionService.updateRolePermissions(1, List.of(1, 999)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.RIGHT_NOT_FOUND.name(), ex.getReason());
            verify(permissionRepository, never()).deleteByRoleIdAndRightIdIn(anyInt(), anyCollection());
        }

        @Test
        void updateRolePermissions_shouldRejectRemovingSystemRight() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            Right business = right(5, "POLICY_VIEW", false);
            when(rightRepository.findAllById(Set.of(5))).thenReturn(List.of(business));
            // Current set contains system right 1, absent from the request
            when(permissionRepository.findRightIdsByRoleId(1)).thenReturn(Set.of(1, 5));
            when(rightRepository.findAllById(Set.of(1)))
                    .thenReturn(List.of(right(1, "PROFILE_VIEW", true)));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> permissionService.updateRolePermissions(1, List.of(5)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.SYSTEM_RIGHT_REQUIRED.name(), ex.getReason());
            verify(permissionRepository, never()).deleteByRoleIdAndRightIdIn(anyInt(), anyCollection());
        }

        @Test
        void updateRolePermissions_shouldDeduplicateRequestedIds() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);
            when(rightRepository.findAllById(Set.of(1))).thenReturn(List.of(right(1, "PROFILE_VIEW", true)));
            when(permissionRepository.findRightIdsByRoleId(1)).thenReturn(Set.of(1));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            RolePermissionsResponse response =
                    permissionService.updateRolePermissions(1, List.of(1, 1, 1));

            assertEquals(List.of(1), response.getRightIds());
            verify(permissionRepository, never()).saveAll(any());
        }
    }
}
