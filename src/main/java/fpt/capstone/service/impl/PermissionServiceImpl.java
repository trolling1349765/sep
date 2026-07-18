package fpt.capstone.service.impl;

import fpt.capstone.dto.response.RolePermissionsResponse;
import fpt.capstone.dto.response.RoleSummaryResponse;
import fpt.capstone.entity.Permission;
import fpt.capstone.entity.Right;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.PermissionRepository;
import fpt.capstone.repository.RightRepository;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.service.PermissionService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    static final String ADMIN_ROLE_NAME = "Admin";

    private final RoleRepository roleRepository;
    private final RightRepository rightRepository;
    private final PermissionRepository permissionRepository;
    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;

    @Override
    public List<RoleSummaryResponse> getRoles() {
        Map<Integer, Long> grantCounts = permissionRepository.countGrantsGroupedByRole().stream()
                .collect(Collectors.toMap(
                        PermissionRepository.RoleGrantCount::getRoleId,
                        PermissionRepository.RoleGrantCount::getGrantedCount));
        return roleRepository.findAll().stream()
                .map(role -> RoleSummaryResponse.from(role, grantCounts.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    @Override
    public RolePermissionsResponse getRolePermissions(int roleId) {
        Role role = requireRole(roleId);
        Set<Integer> rightIds = permissionRepository.findRightIdsByRoleId(roleId);
        return RolePermissionsResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .rightIds(rightIds.stream().sorted().toList())
                .grantedCount(rightIds.size())
                .build();
    }

    @Override
    @Transactional
    public RolePermissionsResponse updateRolePermissions(int roleId, List<Integer> rightIds) {
        Role role = requireRole(roleId);

        if (ADMIN_ROLE_NAME.equalsIgnoreCase(role.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCode.ADMIN_ROLE_LOCKED.name());
        }

        Set<Integer> requested = new HashSet<>(rightIds);
        Map<Integer, Right> rightsById = rightRepository.findAllById(requested).stream()
                .collect(Collectors.toMap(Right::getId, Function.identity()));
        if (rightsById.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.RIGHT_NOT_FOUND.name());
        }

        Set<Integer> current = permissionRepository.findRightIdsByRoleId(roleId);

        Set<Integer> toAdd = new HashSet<>(requested);
        toAdd.removeAll(current);
        Set<Integer> toRemove = new HashSet<>(current);
        toRemove.removeAll(requested);

        boolean removesSystemRight = !toRemove.isEmpty() && rightRepository.findAllById(toRemove).stream()
                .anyMatch(Right::isSystem);
        if (removesSystemRight) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.SYSTEM_RIGHT_REQUIRED.name());
        }

        String actorId = securityUtil.getCurrentUserId();

        if (!toRemove.isEmpty()) {
            permissionRepository.deleteByRoleIdAndRightIdIn(roleId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            List<Permission> newPermissions = new ArrayList<>();
            for (Integer rightId : toAdd) {
                newPermissions.add(Permission.builder()
                        .role(role)
                        .right(rightsById.get(rightId))
                        .createAt(LocalDate.now())
                        .createBy(actorId)
                        .build());
            }
            permissionRepository.saveAll(newPermissions);
        }

        writePermissionUpdateLog(role, actorId, toAdd, toRemove);

        Set<Integer> updated = new HashSet<>(current);
        updated.removeAll(toRemove);
        updated.addAll(toAdd);
        return RolePermissionsResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .rightIds(updated.stream().sorted().toList())
                .grantedCount(updated.size())
                .build();
    }

    private Role requireRole(int roleId) {
        Role role = roleRepository.findById(roleId);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ROLE_NOT_FOUND.name());
        }
        return role;
    }

    private void writePermissionUpdateLog(Role role, String actorId, Set<Integer> added, Set<Integer> removed) {
        try {
            Map<Integer, String> codes = new HashMap<>();
            Set<Integer> all = new HashSet<>(added);
            all.addAll(removed);
            rightRepository.findAllById(all).forEach(r -> codes.put(r.getId(), r.getCode()));

            String diff = "{\"added\":" + toCodeArray(added, codes)
                    + ",\"removed\":" + toCodeArray(removed, codes) + "}";
            systemLogService.write(SystemLog.builder()
                    .userId(actorId)
                    .action(Action.PERMISSION_UPDATE.getAction())
                    .entityType(Table.PERMISSION.getTableName())
                    .entityId(String.valueOf(role.getId()))
                    .newValue(diff)
                    .ipAddress(RequestIpUtil.getCurrentClientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write PERMISSION_UPDATE audit log: {}", e.getMessage());
        }
    }

    private String toCodeArray(Set<Integer> ids, Map<Integer, String> codes) {
        return ids.stream().sorted()
                .map(id -> "\"" + codes.getOrDefault(id, String.valueOf(id)) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
