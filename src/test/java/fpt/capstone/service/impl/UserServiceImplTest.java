package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.SystemLogService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private Role citizenRole;
    private User existingUser;

    @BeforeEach
    void setUp() {
        receptionRole = Role.builder().name("Reception").build();
        receptionRole.setId(2);
        citizenRole = Role.builder().name("Citizen").build();
        citizenRole.setId(1);
        existingUser = User.builder()
                .id("user-1")
                .email("old@example.com")
                .name("Old Name")
                .password("old-hash")
                .role(citizenRole)
                .build();
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        void createRequest_shouldEncodePasswordWithInjectedEncoder() {
            when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("staff")).thenReturn(false);
            when(roleRepository.findById(2)).thenReturn(receptionRole);
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            UserResponse response = userService.createRequest(UserCreationRequest.builder()
                    .role(2).email("staff@example.com").username("staff")
                    .password("Password1").name("Staff").build());

            assertEquals("Reception", response.getRole());
            verify(passwordEncoder).encode("Password1");
            verify(userRepository).save(argThat(u -> "encoded".equals(u.getPassword())
                    && u.getStatus() == fpt.capstone.enums.AccountStatus.ACTIVE));
            verify(systemLogService).write(argThat(log -> "CREATE_USER".equals(log.getAction())));
        }

        @Test
        void createRequest_shouldRejectUnknownRole() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(roleRepository.findById(99)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.createRequest(UserCreationRequest.builder()
                            .role(99).email("a@b.com").username("a").password("Password1").build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ROLE_NOT_FOUND.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createRequest_shouldRejectDuplicateEmailOrUsername() {
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);
            when(userRepository.existsByUsername("dup")).thenReturn(true);

            assertThrows(ArgumentNotValidException.class,
                    () -> userService.createRequest(UserCreationRequest.builder()
                            .role(2).email("dup@example.com").username("dup").password("Password1").build()));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        void updateUser_shouldThrow404WhenUserMissing() {
            when(userRepository.getUserById("ghost")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("ghost", UserUpdateRequest.builder().build()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void updateUser_shouldKeepPasswordWhenBlank() {
            when(userRepository.getUserById("user-1")).thenReturn(existingUser);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            userService.updateUser("user-1", UserUpdateRequest.builder()
                    .name("New Name").password("  ").build());

            assertEquals("old-hash", existingUser.getPassword());
            assertEquals("New Name", existingUser.getName());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void updateUser_shouldEncodeNewPasswordWhenProvided() {
            when(userRepository.getUserById("user-1")).thenReturn(existingUser);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");

            userService.updateUser("user-1", UserUpdateRequest.builder()
                    .password("NewPassword1").build());

            assertEquals("new-hash", existingUser.getPassword());
        }

        @Test
        void updateUser_shouldChangeRoleAndLogChangeRole() {
            when(userRepository.getUserById("user-1")).thenReturn(existingUser);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(2)).thenReturn(receptionRole);

            userService.updateUser("user-1", UserUpdateRequest.builder().roleId(2).build());

            assertEquals("Reception", existingUser.getRole().getName());
            verify(systemLogService).write(argThat(log -> "CHANGE_ROLE".equals(log.getAction())));
        }

        @Test
        void updateUser_shouldRejectChangingOwnRole() {
            when(userRepository.getUserById("admin-1")).thenReturn(User.builder()
                    .id("admin-1").role(citizenRole).build());
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("admin-1", UserUpdateRequest.builder().roleId(2).build()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.CANNOT_CHANGE_OWN_ROLE.name(), ex.getReason());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_shouldAllowSameRoleIdOnOwnAccount() {
            when(userRepository.getUserById("admin-1")).thenReturn(User.builder()
                    .id("admin-1").role(citizenRole).name("Admin").build());
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");

            // roleId equals the current role: not a role change, no guard trip
            assertDoesNotThrow(() -> userService.updateUser("admin-1",
                    UserUpdateRequest.builder().roleId(1).name("Admin Renamed").build()));

            verify(systemLogService).write(argThat(log -> "UPDATE_USER".equals(log.getAction())));
        }

        @Test
        void updateUser_shouldRejectUnknownRoleId() {
            when(userRepository.getUserById("user-1")).thenReturn(existingUser);
            when(securityUtil.getCurrentUserId()).thenReturn("admin-1");
            when(roleRepository.findById(99)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.updateUser("user-1", UserUpdateRequest.builder().roleId(99).build()));

            assertEquals(ErrorCode.ROLE_NOT_FOUND.name(), ex.getReason());
        }
    }
}
