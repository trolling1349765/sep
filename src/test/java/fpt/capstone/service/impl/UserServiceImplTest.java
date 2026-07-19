package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UpdateUserStatusRequest;
import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.AdminUserDetailResponse;
import fpt.capstone.dto.response.AdminUserListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private SystemLogService systemLogService;

    @InjectMocks
    private UserServiceImpl userService;

    private Role receptionRole;
    private Role appraisalRole;
    private Role citizenRole;
    private User staffUser;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        // @Value is not applied outside a Spring context
        ReflectionTestUtils.setField(userService, "defaultAssignedArea", "Xã Minh Tân");

        citizenRole = Role.builder().name("Citizen").code("CITIZEN").build();
        citizenRole.setId(1);
        receptionRole = Role.builder().name("Reception").code("RECEPTION_OFFICER").build();
        receptionRole.setId(2);
        appraisalRole = Role.builder().name("Appraisal").code("APPRAISAL_OFFICER").build();
        appraisalRole.setId(3);

        staffUser = User.builder()
                .id("user-1")
                .email("old@example.com")
                .phone("0900000001")
                .name("Old Name")
                .password("old-hash")
                .role(receptionRole)
                .status(AccountStatus.ACTIVE)
                .build();
        citizenUser = User.builder()
                .id("citizen-1")
                .email("citizen@example.com")
                .name("Citizen User")
                .password("citizen-hash")
                .role(citizenRole)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private UserCreationRequest.UserCreationRequestBuilder validCreation() {
        return UserCreationRequest.builder()
                .roleId(2)
                .email("staff@example.com")
                .username("staff")
                .phone("0912345678")
                .password("Password1")
                .name("Staff")
                .position("Cán bộ tiếp nhận");
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        void createRequest_shouldEncodePasswordAndPersistStaffFields() {
            when(roleRepository.findById(2)).thenReturn(receptionRole);
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            AdminUserDetailResponse response = userService.createRequest(
                    validCreation().assignedArea("Xã Khác").build());

            assertEquals("Reception", response.getRoleName());
            assertEquals("0912345678", response.getPhone());
            assertEquals("Cán bộ tiếp nhận", response.getPosition());
            assertEquals("Xã Khác", response.getAssignedArea());
            assertEquals(AccountStatus.ACTIVE, response.getStatus());
            verify(passwordEncoder).encode("Password1");
            verify(userRepository).save(argThat(u -> "encoded".equals(u.getPassword())
                    && u.getStatus() == AccountStatus.ACTIVE
                    && "0912345678".equals(u.getPhone())
                    && "Cán bộ tiếp nhận".equals(u.getPosition())
                    && "Xã Khác".equals(u.getAssignedArea())));
            verify(systemLogService).write(argThat(log -> "CREATE_USER".equals(log.getAction())));
        }

        @Test
        void createRequest_shouldDefaultAssignedAreaWhenBlank() {
            when(roleRepository.findById(2)).thenReturn(receptionRole);
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            AdminUserDetailResponse response = userService.createRequest(
                    validCreation().assignedArea("  ").build());

            assertEquals("Xã Minh Tân", response.getAssignedArea());
        }

        @Test
        void createRequest_shouldRejectUnknownRole() {
            when(roleRepository.findById(99)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.createRequest(validCreation().roleId(99).build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ROLE_NOT_FOUND.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createRequest_shouldRejectCitizenRole() {
            when(roleRepository.findById(1)).thenReturn(citizenRole);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.createRequest(validCreation().roleId(1).build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.CITIZEN_ROLE_RESTRICTED.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createRequest_shouldGatherAllThreeDuplicateErrors() {
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);
            when(userRepository.existsByUsername("dup")).thenReturn(true);
            when(userRepository.existsByPhone("0912345678")).thenReturn(true);

            ArgumentNotValidException ex = assertThrows(ArgumentNotValidException.class,
                    () -> userService.createRequest(validCreation()
                            .email("dup@example.com").username("dup").build()));

            assertEquals(3, ex.getResponses().size());
            List<Integer> codes = ex.getResponses().stream()
                    .map(fpt.capstone.dto.response.APIResponse::getCode).toList();
            assertTrue(codes.contains(ErrorCode.EMAIL_EXISTED.getCode()));
            assertTrue(codes.contains(ErrorCode.USERNAME_EXISTED.getCode()));
            assertTrue(codes.contains(ErrorCode.PHONE_EXISTED.getCode()));
            verify(userRepository, never()).save(any());
        }

        @Test
        void createRequest_shouldRejectDuplicatePhoneOnly() {
            when(userRepository.existsByPhone("0912345678")).thenReturn(true);

            ArgumentNotValidException ex = assertThrows(ArgumentNotValidException.class,
                    () -> userService.createRequest(validCreation().build()));

            assertEquals(1, ex.getResponses().size());
            assertEquals(ErrorCode.PHONE_EXISTED.getCode(), ex.getResponses().get(0).getCode());
        }

        @Test
        void createRequest_shouldUseEmptyCreatorWhenActorNull() {
            when(roleRepository.findById(2)).thenReturn(receptionRole);
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(securityUtil.getCurrentUserId()).thenReturn(null);

            userService.createRequest(validCreation().build());

            // actorId null -> createBy falls back to "" rather than null
            verify(userRepository).save(argThat(u -> "".equals(u.getCreateBy())));
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        void updateUser_shouldThrow404WhenUserMissing() {
            when(userRepository.findWithRoleById("ghost")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("ghost", UserUpdateRequest.builder().build()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void updateUser_shouldKeepPasswordWhenBlank() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            userService.updateUser("user-1", UserUpdateRequest.builder()
                    .name("New Name").password("  ").build());

            assertEquals("old-hash", staffUser.getPassword());
            assertEquals("New Name", staffUser.getName());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void updateUser_shouldEncodeNewPasswordWhenProvided() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

            userService.updateUser("user-1", UserUpdateRequest.builder()
                    .password("NewPassword1").build());

            assertEquals("new-hash", staffUser.getPassword());
        }

        @Test
        void updateUser_shouldPatchPhonePositionAssignedArea() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(userRepository.existsByPhoneAndIdNot("0987654321", "user-1")).thenReturn(false);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            userService.updateUser("user-1", UserUpdateRequest.builder()
                    .phone("0987654321").position("Trưởng bộ phận").assignedArea("Xã Mới").build());

            assertEquals("0987654321", staffUser.getPhone());
            assertEquals("Trưởng bộ phận", staffUser.getPosition());
            assertEquals("Xã Mới", staffUser.getAssignedArea());
        }

        @Test
        void updateUser_shouldRejectDuplicateEmailExcludingSelf() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(userRepository.existsByEmailAndIdNot("taken@example.com", "user-1")).thenReturn(true);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ArgumentNotValidException ex = assertThrows(ArgumentNotValidException.class,
                    () -> userService.updateUser("user-1", UserUpdateRequest.builder()
                            .email("taken@example.com").build()));

            assertEquals(ErrorCode.EMAIL_EXISTED.getCode(), ex.getResponses().get(0).getCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_shouldAllowKeepingOwnEmail() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(userRepository.existsByEmailAndIdNot("old@example.com", "user-1")).thenReturn(false);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            assertDoesNotThrow(() -> userService.updateUser("user-1", UserUpdateRequest.builder()
                    .email("old@example.com").build()));

            verify(userRepository).save(staffUser);
        }

        @Test
        void updateUser_shouldRejectDuplicatePhoneExcludingSelf() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(userRepository.existsByPhoneAndIdNot("0999999999", "user-1")).thenReturn(true);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ArgumentNotValidException ex = assertThrows(ArgumentNotValidException.class,
                    () -> userService.updateUser("user-1", UserUpdateRequest.builder()
                            .phone("0999999999").build()));

            assertEquals(ErrorCode.PHONE_EXISTED.getCode(), ex.getResponses().get(0).getCode());
        }

        @Test
        void updateUser_shouldChangeRoleAndLogChangeRole() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(3)).thenReturn(appraisalRole);

            userService.updateUser("user-1", UserUpdateRequest.builder().roleId(3).build());

            assertEquals("Appraisal", staffUser.getRole().getName());
            verify(systemLogService).write(argThat(log -> "CHANGE_ROLE".equals(log.getAction())
                    && "Reception -> Appraisal".equals(log.getNewValue())));
        }

        @Test
        void updateUser_shouldRejectChangingOwnRole() {
            when(userRepository.findWithRoleById("admin-1")).thenReturn(Optional.of(User.builder()
                    .id("admin-1").role(receptionRole).build()));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("admin-1", UserUpdateRequest.builder().roleId(3).build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.CANNOT_CHANGE_OWN_ROLE.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_shouldAllowSameRoleIdOnOwnAccount() {
            when(userRepository.findWithRoleById("admin-1")).thenReturn(Optional.of(User.builder()
                    .id("admin-1").role(receptionRole).name("Admin").build()));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            // roleId equals the current role: not a role change, no guard trip
            assertDoesNotThrow(() -> userService.updateUser("admin-1",
                    UserUpdateRequest.builder().roleId(2).name("Admin Renamed").build()));

            verify(systemLogService).write(argThat(log -> "UPDATE_USER".equals(log.getAction())));
        }

        @Test
        void updateUser_shouldRejectUnknownRoleId() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(99)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("user-1", UserUpdateRequest.builder().roleId(99).build()));

            assertEquals(ErrorCode.ROLE_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void updateUser_shouldRejectAssigningCitizenRole() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(1)).thenReturn(citizenRole);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("user-1", UserUpdateRequest.builder().roleId(1).build()));

            assertEquals(ErrorCode.CITIZEN_ROLE_RESTRICTED.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_shouldRejectRoleChangeOfCitizenUser() {
            when(userRepository.findWithRoleById("citizen-1")).thenReturn(Optional.of(citizenUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("citizen-1", UserUpdateRequest.builder().roleId(2).build()));

            assertEquals(ErrorCode.CITIZEN_ROLE_RESTRICTED.name(), ex.getReason());
            // Guarded before the new role is even loaded
            verify(roleRepository, never()).findById(2);
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_shouldAssignRoleWhenUserHasNoRole() {
            User noRole = User.builder().id("nr-1").role(null).status(AccountStatus.ACTIVE).build();
            when(userRepository.findWithRoleById("nr-1")).thenReturn(Optional.of(noRole));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(3)).thenReturn(appraisalRole);

            userService.updateUser("nr-1", UserUpdateRequest.builder().roleId(3).build());

            assertEquals("Appraisal", noRole.getRole().getName());
            // Old role name is null when the user had none
            verify(systemLogService).write(argThat(log -> "CHANGE_ROLE".equals(log.getAction())
                    && "null -> Appraisal".equals(log.getNewValue())));
        }

        @Test
        void updateUser_shouldSetDobWhenProvided() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            LocalDate dob = LocalDate.of(1990, 5, 20);

            userService.updateUser("user-1", UserUpdateRequest.builder().dob(dob).build());

            assertEquals(dob, staffUser.getDob());
        }

        @Test
        void updateUser_shouldSwallowAuditLogFailure() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            doThrow(new RuntimeException("log down")).when(systemLogService).write(any());

            AdminUserDetailResponse response = assertDoesNotThrow(() -> userService.updateUser("user-1",
                    UserUpdateRequest.builder().name("New Name").build()));

            assertEquals("New Name", response.getName());
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        void searchUsers_shouldMapCreatedAtSortToEntityFieldWithIdTiebreaker() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(userRepository.search(isNull(), isNull(), isNull(), pageableCaptor.capture()))
                    .thenAnswer(inv -> new PageImpl<>(List.of(staffUser), inv.getArgument(3), 1));

            PageResponse<AdminUserListResponse> result =
                    userService.searchUsers(0, 20, null, null, null, "createdAt", "desc");

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(Sort.Order.desc("createAt"), pageable.getSort().getOrderFor("createAt"));
            assertEquals(Sort.Order.asc("id"), pageable.getSort().getOrderFor("id"));
            assertEquals(1, result.getTotalElements());
            assertEquals("user-1", result.getItems().get(0).getId());
            assertEquals("Reception", result.getItems().get(0).getRoleName());
        }

        @Test
        void searchUsers_shouldSortUsernameAscending() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(userRepository.search(any(), isNull(), isNull(), pageableCaptor.capture()))
                    .thenAnswer(inv -> new PageImpl<>(List.<User>of(), inv.getArgument(3), 0));

            userService.searchUsers(0, 20, "demo", null, null, "username", "asc");

            assertEquals(Sort.Order.asc("username"),
                    pageableCaptor.getValue().getSort().getOrderFor("username"));
        }

        @Test
        void searchUsers_shouldTrimQueryAndPassNullWhenBlank() {
            ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
            when(userRepository.search(qCaptor.capture(), isNull(), isNull(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.<User>of(), inv.getArgument(3), 0));

            userService.searchUsers(0, 20, "   ", null, null, "createdAt", "desc");
            userService.searchUsers(0, 20, "  demo ", null, null, "createdAt", "desc");

            assertNull(qCaptor.getAllValues().get(0));
            assertEquals("demo", qCaptor.getAllValues().get(1));
        }

        @Test
        void searchUsers_shouldClampSizeTo100() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(userRepository.search(isNull(), isNull(), isNull(), pageableCaptor.capture()))
                    .thenAnswer(inv -> new PageImpl<>(List.<User>of(), inv.getArgument(3), 0));

            userService.searchUsers(0, 1000, null, null, null, "createdAt", "desc");

            assertEquals(100, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void searchUsers_shouldRejectNonWhitelistedSort() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.searchUsers(0, 20, null, null, null, "password", "desc"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verifyNoInteractions(userRepository);
        }

        @Test
        void searchUsers_shouldRejectInvalidDirection() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.searchUsers(0, 20, null, null, null, "username", "sideways"));

            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void searchUsers_shouldRejectNegativePageAndZeroSize() {
            assertThrows(ResponseStatusException.class,
                    () -> userService.searchUsers(-1, 20, null, null, null, "createdAt", "desc"));
            assertThrows(ResponseStatusException.class,
                    () -> userService.searchUsers(0, 0, null, null, null, "createdAt", "desc"));
            verifyNoInteractions(userRepository);
        }

        @Test
        void searchUsers_shouldPassFiltersThrough() {
            when(userRepository.search(isNull(), eq(2), eq(AccountStatus.INACTIVE), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.<User>of(), inv.getArgument(3), 0));

            PageResponse<AdminUserListResponse> result =
                    userService.searchUsers(0, 20, null, 2, AccountStatus.INACTIVE, "createdAt", "desc");

            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        void changeStatus_shouldRejectInvalidLiterals() {
            for (String bad : new String[] { "BANNED", "VERIFIED", "active", "garbage", null }) {
                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                        () -> userService.changeStatus("user-1",
                                UpdateUserStatusRequest.builder().status(bad).build()));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertEquals(ErrorCode.INVALID_STATUS.name(), ex.getReason());
            }
            verifyNoInteractions(userRepository);
        }

        @Test
        void changeStatus_shouldThrow404WhenUserMissing() {
            when(userRepository.findWithRoleById("ghost")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.changeStatus("ghost",
                            UpdateUserStatusRequest.builder().status("INACTIVE").build()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void changeStatus_shouldRejectSelfDeactivation() {
            when(userRepository.findWithRoleById("admin-1")).thenReturn(Optional.of(User.builder()
                    .id("admin-1").role(receptionRole).status(AccountStatus.ACTIVE).build()));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.changeStatus("admin-1",
                            UpdateUserStatusRequest.builder().status("INACTIVE").build()));

            assertEquals(ErrorCode.CANNOT_DEACTIVATE_SELF.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void changeStatus_shouldRejectUnchangedStatus() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            InvalidArgsException ex = assertThrows(InvalidArgsException.class,
                    () -> userService.changeStatus("user-1",
                            UpdateUserStatusRequest.builder().status("ACTIVE").build()));

            assertEquals(ErrorCode.INVALID_STATUS.getCode(), ex.getResponse().getCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        void changeStatus_shouldDeactivateAndAuditWithOldAndNewValue() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            AdminUserDetailResponse response = userService.changeStatus("user-1",
                    UpdateUserStatusRequest.builder().status("INACTIVE").build());

            assertEquals(AccountStatus.INACTIVE, response.getStatus());
            verify(userRepository).save(staffUser);
            verify(systemLogService).write(argThat(log -> "USER_DEACTIVATE".equals(log.getAction())
                    && "ACTIVE".equals(log.getOldValue())
                    && "INACTIVE".equals(log.getNewValue())
                    && "user-1".equals(log.getEntityId())));
        }

        @Test
        void changeStatus_shouldReactivateAndAudit() {
            staffUser.setStatus(AccountStatus.INACTIVE);
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            AdminUserDetailResponse response = userService.changeStatus("user-1",
                    UpdateUserStatusRequest.builder().status("ACTIVE").build());

            assertEquals(AccountStatus.ACTIVE, response.getStatus());
            verify(systemLogService).write(argThat(log -> "USER_ACTIVATE".equals(log.getAction())
                    && "INACTIVE".equals(log.getOldValue())
                    && "ACTIVE".equals(log.getNewValue())));
        }

        @Test
        void changeStatus_shouldHandleNullOldStatus() {
            User noStatus = User.builder().id("ns-1").role(receptionRole).status(null).build();
            when(userRepository.findWithRoleById("ns-1")).thenReturn(Optional.of(noStatus));
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            AdminUserDetailResponse response = userService.changeStatus("ns-1",
                    UpdateUserStatusRequest.builder().status("ACTIVE").build());

            assertEquals(AccountStatus.ACTIVE, response.getStatus());
            // oldStatus null -> oldValue logged as null, not a name
            verify(systemLogService).write(argThat(log -> "USER_ACTIVATE".equals(log.getAction())
                    && log.getOldValue() == null
                    && "ACTIVE".equals(log.getNewValue())));
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        void getUser_shouldReturnDetailWithRoleAndStaffFields() {
            staffUser.setPosition("Cán bộ tiếp nhận");
            staffUser.setAssignedArea("Xã Minh Tân");
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(staffUser));

            AdminUserDetailResponse response = userService.getUser("user-1");

            assertEquals("user-1", response.getId());
            assertEquals(2, response.getRoleId());
            assertEquals("Reception", response.getRoleName());
            assertEquals("Cán bộ tiếp nhận", response.getPosition());
            assertEquals("Xã Minh Tân", response.getAssignedArea());
        }

        @Test
        void getUser_shouldThrow404WhenMissing() {
            when(userRepository.findWithRoleById("ghost")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.getUser("ghost"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void getUser_shouldTolerateRoleLessUser() {
            when(userRepository.findWithRoleById("user-1")).thenReturn(Optional.of(User.builder()
                    .id("user-1").name("No Role").status(AccountStatus.ACTIVE).build()));

            AdminUserDetailResponse response = userService.getUser("user-1");

            assertNull(response.getRoleId());
            assertNull(response.getRoleName());
        }
    }
}
