package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UpdateProfileRequest;
import fpt.capstone.dto.response.UserProfileResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.User;
import fpt.capstone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser;
    private final String testUserId = "test-user-id";
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        Role testRole = new Role();
        testRole.setId(2);
        testRole.setName("USER");

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("Test User");
        testUser.setEmail(testEmail);
        testUser.setPhone("0123456789");
        testUser.setNationalId("123456789012");
        testUser.setDob(LocalDate.of(2000, 1, 1));
        testUser.setGender(true);
        testUser.setProvinceCode("01");
        testUser.setProvinceName("Hanoi");
        testUser.setWardCode("001");
        testUser.setWardName("Ward 1");
        testUser.setSpecificAddress("123 Main St");
        testUser.setRole(testRole);
    }

    // ============================================================
    // GET PROFILE TESTS
    // ============================================================
    @Nested
    class GetProfileTests {

        @Test
        void getProfile_success() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(testUser);

            // Act
            UserProfileResponse response = userProfileService.getProfile(testUserId);

            // Assert
            assertNotNull(response);
            assertEquals(testUserId, response.getUserId());
            assertEquals("Test User", response.getName());
            assertEquals(testEmail, response.getEmail());
            assertEquals("0123456789", response.getPhone());
            assertEquals("123456789012", response.getNationalId());
            assertEquals(LocalDate.of(2000, 1, 1), response.getDob());
            assertEquals(true, response.getGender());
            assertEquals("USER", response.getRole());
            assertEquals("123 Main St, Ward 1, Hanoi", response.getFullAddress());
        }

        @Test
        void getProfile_userNotFound() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(null);

            // Act & Assert
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userProfileService.getProfile(testUserId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getReason().contains("User not found"));
        }
    }

    // ============================================================
    // UPDATE PROFILE TESTS
    // ============================================================
    @Nested
    class UpdateProfileTests {

        @Test
        void updateProfile_allFields() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(testUser);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .name("Updated Name")
                    .email("updated@example.com")
                    .phone("0987654321")
                    .nationalId("987654321098")
                    .dob(LocalDate.of(1995, 5, 15))
                    .gender(false)
                    .provinceCode("02")
                    .provinceName("Ho Chi Minh")
                    .wardCode("002")
                    .wardName("Ward 2")
                    .specificAddress("456 Oak Ave")
                    .build();

            // Act
            UserProfileResponse response = userProfileService.updateProfile(testUserId, request);

            // Assert
            assertNotNull(response);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertEquals("Updated Name", savedUser.getName());
            assertEquals("updated@example.com", savedUser.getEmail());
            assertEquals("0987654321", savedUser.getPhone());
            assertEquals("987654321098", savedUser.getNationalId());
            assertEquals(LocalDate.of(1995, 5, 15), savedUser.getDob());
            assertEquals(false, savedUser.getGender());
            assertEquals("02", savedUser.getProvinceCode());
            assertEquals("Ho Chi Minh", savedUser.getProvinceName());
            assertEquals("002", savedUser.getWardCode());
            assertEquals("Ward 2", savedUser.getWardName());
            assertEquals("456 Oak Ave", savedUser.getSpecificAddress());
        }

        @Test
        void updateProfile_partialFields() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(testUser);
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .name("Only Name Updated")
                    .build();

            // Act
            UserProfileResponse response = userProfileService.updateProfile(testUserId, request);

            // Assert
            assertNotNull(response);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertEquals("Only Name Updated", savedUser.getName());
            // Other fields should remain unchanged
            assertEquals(testEmail, savedUser.getEmail());
            assertEquals("0123456789", savedUser.getPhone());
            assertEquals("123456789012", savedUser.getNationalId());
            assertEquals(LocalDate.of(2000, 1, 1), savedUser.getDob());
            assertEquals(true, savedUser.getGender());
        }

        @Test
        void updateProfile_nullFields_shouldNotChange() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(testUser);
            UpdateProfileRequest request = new UpdateProfileRequest(); // all fields null

            // Act
            UserProfileResponse response = userProfileService.updateProfile(testUserId, request);

            // Assert
            assertNotNull(response);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            // All fields should remain as original
            assertEquals("Test User", savedUser.getName());
            assertEquals(testEmail, savedUser.getEmail());
            assertEquals("0123456789", savedUser.getPhone());
            assertEquals("123456789012", savedUser.getNationalId());
        }

        @Test
        void updateProfile_userNotFound() {
            // Arrange
            when(userRepository.getUserById(testUserId)).thenReturn(null);
            UpdateProfileRequest request = new UpdateProfileRequest();

            // Act & Assert
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userProfileService.updateProfile(testUserId, request));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getReason().contains("User not found"));
            verify(userRepository, never()).save(any());
        }
    }
}