package fpt.capstone.service.impl;

import fpt.capstone.dto.request.CreateRightRequest;
import fpt.capstone.dto.request.UpdateRightRequest;
import fpt.capstone.dto.response.RightModuleResponse;
import fpt.capstone.dto.response.RightResponse;
import fpt.capstone.entity.Right;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.RightRepository;
import fpt.capstone.service.RightService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.RequestIpUtil;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RightServiceImpl implements RightService {

    static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    private final RightRepository rightRepository;
    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;

    @Override
    public List<RightModuleResponse> getCatalogue() {
        Map<String, RightModuleResponse> byModule = new LinkedHashMap<>();
        for (Right right : rightRepository.findAllByOrderByModuleAscSortOrderAsc()) {
            byModule.computeIfAbsent(right.getModule(), module -> RightModuleResponse.builder()
                    .module(module)
                    .moduleName(right.getModuleName())
                    .rights(new ArrayList<>())
                    .build())
                    .getRights().add(RightResponse.from(right));
        }
        return new ArrayList<>(byModule.values());
    }

    @Override
    @Transactional
    public RightResponse createRight(CreateRightRequest request) {
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_RIGHT_CODE.name());
        }
        if (rightRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorCode.RIGHT_CODE_EXISTS.name());
        }

        String module = request.getModule().trim();
        // Reuse the module display name when extending an existing module
        String moduleName = rightRepository.findAllByOrderByModuleAscSortOrderAsc().stream()
                .filter(r -> module.equals(r.getModule()))
                .map(Right::getModuleName)
                .findFirst()
                .orElse(request.getModuleName());
        int nextSortOrder = (int) rightRepository.findAllByOrderByModuleAscSortOrderAsc().stream()
                .filter(r -> module.equals(r.getModule()))
                .count() + 1;

        Right right = Right.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .module(module)
                .moduleName(moduleName)
                // API-created rights are never system rights (spec 9.5)
                .isSystem(false)
                .sortOrder(nextSortOrder)
                .createAt(LocalDate.now())
                .createBy(securityUtil.getCurrentUserId())
                .build();
        right = rightRepository.save(right);

        writeRightLog(Action.RIGHT_CREATE, right);
        return RightResponse.from(right);
    }

    @Override
    @Transactional
    public RightResponse updateRight(int rightId, UpdateRightRequest request) {
        Right right = rightRepository.findById(rightId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCode.RIGHT_NOT_FOUND.name()));
        if (request.getName() != null && !request.getName().isBlank()) {
            right.setName(request.getName());
        }
        if (request.getDescription() != null) {
            right.setDescription(request.getDescription());
        }
        right.setUpdateAt(LocalDate.now());
        right.setUpdateBy(securityUtil.getCurrentUserId());
        right = rightRepository.save(right);

        writeRightLog(Action.RIGHT_UPDATE, right);
        return RightResponse.from(right);
    }

    private void writeRightLog(Action action, Right right) {
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(securityUtil.getCurrentUserId())
                    .action(action.getAction())
                    .entityType(Table.RIGHT.getTableName())
                    .entityId(String.valueOf(right.getId()))
                    .newValue(right.getCode())
                    .ipAddress(RequestIpUtil.getCurrentClientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write {} audit log: {}", action, e.getMessage());
        }
    }
}
