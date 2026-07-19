package fpt.capstone.service.impl;

import fpt.capstone.config.UptimeTracker;
import fpt.capstone.dto.response.DashboardResponse;
import fpt.capstone.dto.response.RecentActivityResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemLogRepository systemLogRepository;
    @Mock
    private UptimeTracker uptimeTracker;
    @Mock
    private ObjectProvider<BuildProperties> buildProperties;
    @Mock
    private UserRepository.DashboardUserStats stats;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                userRepository,
                systemLogRepository,
                uptimeTracker,
                buildProperties,
                "DEMO",
                "OCR,EMAIL,SMS,DIGITAL_SIGNATURE");
    }

    private void stubHappyPath() {
        when(userRepository.countDashboardStats(any(Instant.class))).thenReturn(stats);
        when(systemLogRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(List.of());
        when(buildProperties.stream()).thenReturn(Stream.empty());
        when(uptimeTracker.getStartedAt()).thenReturn(Instant.parse("2026-07-18T21:00:05Z"));
    }

    private SystemLog log(int id, String userId) {
        return SystemLog.builder()
                .id(id)
                .userId(userId)
                .action("USER_LOGIN")
                .entityType("USERS")
                .newValue("SUCCESS")
                .createdAt(LocalDateTime.of(2026, 7, 19, 4, 38, 12))
                .build();
    }

    private User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    @Nested
    class RecentSizeValidation {

        @Test
        void getDashboard_recentSizeZero_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> dashboardService.getDashboard(0));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verifyNoInteractions(userRepository, systemLogRepository);
        }

        @Test
        void getDashboard_recentSizeNegative_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> dashboardService.getDashboard(-3));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void getDashboard_recentSizeOverMax_clampedToTwenty() {
            stubHappyPath();

            dashboardService.getDashboard(25);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(systemLogRepository).findAllByOrderByCreatedAtDescIdDesc(pageableCaptor.capture());
            assertEquals(20, pageableCaptor.getValue().getPageSize());
            assertEquals(0, pageableCaptor.getValue().getPageNumber());
        }

        @Test
        void getDashboard_defaultFive_passedThrough() {
            stubHappyPath();

            dashboardService.getDashboard(5);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(systemLogRepository).findAllByOrderByCreatedAtDescIdDesc(pageableCaptor.capture());
            assertEquals(5, pageableCaptor.getValue().getPageSize());
        }
    }

    @Nested
    class AccountStats {

        @Test
        void getDashboard_mapsAllStatFields() {
            stubHappyPath();
            when(stats.getTotal()).thenReturn(8L);
            when(stats.getActiveCount()).thenReturn(6L);
            when(stats.getPendingVerification()).thenReturn(1L);
            when(stats.getLockedCount()).thenReturn(2L);
            when(stats.getBannedCount()).thenReturn(1L);
            when(stats.getTempLockedCount()).thenReturn(1L);

            DashboardResponse response = dashboardService.getDashboard(5);

            assertEquals(8, response.getAccounts().getTotal());
            assertEquals(6, response.getAccounts().getActive());
            assertEquals(1, response.getAccounts().getPendingVerification());
            assertEquals(2, response.getAccounts().getLocked());
            assertEquals(1, response.getAccounts().getBanned());
            assertEquals(1, response.getAccounts().getTempLocked());
        }

        @Test
        void getDashboard_nullSums_mappedToZero() {
            stubHappyPath();
            // SUM over an empty table can come back null

            DashboardResponse response = dashboardService.getDashboard(5);

            assertEquals(0, response.getAccounts().getTotal());
            assertEquals(0, response.getAccounts().getActive());
            assertEquals(0, response.getAccounts().getPendingVerification());
            assertEquals(0, response.getAccounts().getLocked());
            assertEquals(0, response.getAccounts().getBanned());
            assertEquals(0, response.getAccounts().getTempLocked());
        }
    }

    @Nested
    class RecentActivities {

        @Test
        void getDashboard_actorNamesResolvedInOneBatch() {
            when(userRepository.countDashboardStats(any(Instant.class))).thenReturn(stats);
            when(buildProperties.stream()).thenReturn(Stream.empty());
            when(uptimeTracker.getStartedAt()).thenReturn(Instant.parse("2026-07-18T21:00:05Z"));
            when(systemLogRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                    .thenReturn(List.of(log(3, "u1"), log(2, "u2"), log(1, null)));
            when(userRepository.findAllById(List.of("u1", "u2")))
                    .thenReturn(List.of(user("u1", "Hoàng Minh Demo")));

            DashboardResponse response = dashboardService.getDashboard(5);

            List<RecentActivityResponse> activities = response.getRecentActivities();
            assertEquals(3, activities.size());
            // exactly one batch lookup, only distinct non-null ids
            verify(userRepository, times(1)).findAllById(List.of("u1", "u2"));
            assertEquals("Hoàng Minh Demo", activities.get(0).getActorName());
            // u2 was deleted/unknown -> null
            assertNull(activities.get(1).getActorName());
            // system event (null userId) -> null actor
            assertNull(activities.get(2).getActorName());
            assertNull(activities.get(2).getActorId());
            assertEquals("USER_LOGIN", activities.get(0).getAction());
            assertEquals("SUCCESS", activities.get(0).getDetail());
        }

        @Test
        void getDashboard_noLogs_skipsUserLookup() {
            stubHappyPath();

            DashboardResponse response = dashboardService.getDashboard(5);

            assertTrue(response.getRecentActivities().isEmpty());
            verify(userRepository, never()).findAllById(any());
        }
    }

    @Nested
    class OperationalStatus {

        @Test
        void getDashboard_versionFallsBackToDev() {
            stubHappyPath();

            DashboardResponse response = dashboardService.getDashboard(5);

            assertEquals("UP", response.getOperational().getStatus());
            assertEquals("dev", response.getOperational().getVersion());
            assertEquals("DEMO", response.getOperational().getEnvironment());
            assertEquals(Instant.parse("2026-07-18T21:00:05Z"), response.getOperational().getStartedAt());
            assertNotNull(response.getOperational().getServerTime());
            assertEquals(List.of("OCR", "EMAIL", "SMS", "DIGITAL_SIGNATURE"),
                    response.getOperational().getSimulatedServices());
        }

        @Test
        void getDashboard_versionFromBuildProperties() {
            when(userRepository.countDashboardStats(any(Instant.class))).thenReturn(stats);
            when(systemLogRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                    .thenReturn(List.of());
            when(uptimeTracker.getStartedAt()).thenReturn(Instant.parse("2026-07-18T21:00:05Z"));
            BuildProperties props = mock(BuildProperties.class);
            when(props.getVersion()).thenReturn("0.0.1-SNAPSHOT");
            when(buildProperties.stream()).thenReturn(Stream.of(props));

            DashboardResponse response = dashboardService.getDashboard(5);

            assertEquals("0.0.1-SNAPSHOT", response.getOperational().getVersion());
        }
    }
}
