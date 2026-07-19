package fpt.capstone.service.impl;

import fpt.capstone.config.UptimeTracker;
import fpt.capstone.dto.response.AccountStatsResponse;
import fpt.capstone.dto.response.DashboardResponse;
import fpt.capstone.dto.response.OperationalStatusResponse;
import fpt.capstone.dto.response.RecentActivityResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.DashboardService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Read-only aggregation for the admin overview screen. Deliberately has no
// SystemLogService dependency: viewing the dashboard must never write audit
// rows (FE polls this endpoint).
@Service
public class DashboardServiceImpl implements DashboardService {

    static final int MAX_RECENT_SIZE = 20;
    static final String STATUS_UP = "UP";
    static final String VERSION_FALLBACK = "dev";

    private final UserRepository userRepository;
    private final SystemLogRepository systemLogRepository;
    private final UptimeTracker uptimeTracker;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final String environment;
    private final List<String> simulatedServices;

    public DashboardServiceImpl(
            UserRepository userRepository,
            SystemLogRepository systemLogRepository,
            UptimeTracker uptimeTracker,
            ObjectProvider<BuildProperties> buildProperties,
            @Value("${app.environment:DEMO}") String environment,
            @Value("${app.simulated-services:OCR,EMAIL,SMS,DIGITAL_SIGNATURE}") String simulatedServices) {
        this.userRepository = userRepository;
        this.systemLogRepository = systemLogRepository;
        this.uptimeTracker = uptimeTracker;
        this.buildProperties = buildProperties;
        this.environment = environment;
        this.simulatedServices = Arrays.stream(simulatedServices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(int recentSize) {
        if (recentSize < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        int size = Math.min(recentSize, MAX_RECENT_SIZE);

        return DashboardResponse.builder()
                .accounts(loadAccountStats())
                .recentActivities(loadRecentActivities(size))
                .operational(buildOperationalStatus())
                .build();
    }

    private AccountStatsResponse loadAccountStats() {
        UserRepository.DashboardUserStats stats = userRepository.countDashboardStats(Instant.now());
        return AccountStatsResponse.builder()
                .total(zeroIfNull(stats.getTotal()))
                .active(zeroIfNull(stats.getActiveCount()))
                .pendingVerification(zeroIfNull(stats.getPendingVerification()))
                .locked(zeroIfNull(stats.getLockedCount()))
                .banned(zeroIfNull(stats.getBannedCount()))
                .tempLocked(zeroIfNull(stats.getTempLockedCount()))
                .build();
    }

    private List<RecentActivityResponse> loadRecentActivities(int size) {
        List<SystemLog> logs = systemLogRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, size));

        // Resolve actor names in one batch query - no N+1. Missing or null
        // userId -> actorName null (FE renders "Hệ thống").
        List<String> actorIds = logs.stream()
                .map(SystemLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> namesById = actorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        return logs.stream()
                .map(log -> RecentActivityResponse.from(log,
                        log.getUserId() == null ? null : namesById.get(log.getUserId())))
                .toList();
    }

    private OperationalStatusResponse buildOperationalStatus() {
        String version = buildProperties.stream()
                .map(BuildProperties::getVersion)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(VERSION_FALLBACK);
        return OperationalStatusResponse.builder()
                .status(STATUS_UP)
                .serverTime(LocalDateTime.now())
                .startedAt(uptimeTracker.getStartedAt())
                .version(version)
                .environment(environment)
                .simulatedServices(simulatedServices)
                .build();
    }

    private long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
