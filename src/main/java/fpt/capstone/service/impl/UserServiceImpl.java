package fpt.capstone.service.impl;

import fpt.capstone.config.RoleCodes;
import fpt.capstone.dto.request.UpdateUserStatusRequest;
import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AdminUserDetailResponse;
import fpt.capstone.dto.response.AdminUserListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.service.UserService;
import fpt.capstone.util.RequestIpUtil;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 100;

    // API sort key -> entity property. Whitelist so clients can't sort by
    // arbitrary fields (password, lockedUntil...) — sort injection guard.
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "username", "username",
            "createdAt", "createAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final SystemLogService systemLogService;

    @Value("${app.default-assigned-area:Xã Minh Tân}")
    private String defaultAssignedArea;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserListResponse> searchUsers(int page, int size, String q,
                                                           Integer roleId, AccountStatus status,
                                                           String sort, String dir) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        String property = SORT_WHITELIST.get(sort);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }

        String query = (q == null || q.isBlank()) ? null : q.trim();
        // Stable paging tiebreaker on id; the repository query has no order by
        // of its own so this Sort is the only ordering applied.
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));

        Page<User> result = userRepository.search(query, roleId, status, pageable);
        Instant now = Instant.now();
        return PageResponse.from(result.map(user -> AdminUserListResponse.from(user, now)));
    }

    @Override
    @Transactional
    public AdminUserDetailResponse createRequest(UserCreationRequest request) {

        List<APIResponse> exceptions = new ArrayList<>();

        if (userRepository.existsByEmail(request.getEmail())) {
            APIResponse response = new APIResponse<>();
            response.setCode(ErrorCode.EMAIL_EXISTED.getCode());
            response.setMessage(ErrorCode.EMAIL_EXISTED.getMessage());
            response.setData(request.getEmail());
            exceptions.add(response);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            APIResponse response = new APIResponse<>();
            response.setCode(ErrorCode.USERNAME_EXISTED.getCode());
            response.setMessage(ErrorCode.USERNAME_EXISTED.getMessage());
            response.setData(request.getUsername());
            exceptions.add(response);
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            APIResponse response = new APIResponse<>();
            response.setCode(ErrorCode.PHONE_EXISTED.getCode());
            response.setMessage(ErrorCode.PHONE_EXISTED.getMessage());
            response.setData(request.getPhone());
            exceptions.add(response);
        }
        if (!exceptions.isEmpty()) {
            throw new ArgumentNotValidException(exceptions);
        }

        Role role = roleRepository.findById(request.getRoleId().intValue());
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND.name());
        }
        // Citizen accounts only come from self-registration (spec decision #2);
        // the FE hiding Citizen in the dropdown is UX, this is the enforcement.
        if (RoleCodes.CITIZEN.equals(role.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.CITIZEN_ROLE_RESTRICTED.name());
        }

        String actorId = securityUtil.getCurrentUserId();
        User user = User.builder()
                .role(role)
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .position(request.getPosition())
                .assignedArea(isBlank(request.getAssignedArea())
                        ? defaultAssignedArea
                        : request.getAssignedArea())
                .dob(request.getDob())
                .status(AccountStatus.ACTIVE)
                .createAt(LocalDate.now())
                .createBy(actorId != null ? actorId : "")
                .build();

        userRepository.save(user);
        writeLog(Action.CREATE_USER, user.getId(), actorId, role.getName());
        return AdminUserDetailResponse.from(user);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCode.USERNAME_NOT_EXISTED.name()));

        String actorId = securityUtil.getCurrentUserId();

        List<APIResponse> exceptions = new ArrayList<>();
        if (!isBlank(request.getEmail())
                && userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            APIResponse response = new APIResponse<>();
            response.setCode(ErrorCode.EMAIL_EXISTED.getCode());
            response.setMessage(ErrorCode.EMAIL_EXISTED.getMessage());
            response.setData(request.getEmail());
            exceptions.add(response);
        }
        if (!isBlank(request.getPhone())
                && userRepository.existsByPhoneAndIdNot(request.getPhone(), userId)) {
            APIResponse response = new APIResponse<>();
            response.setCode(ErrorCode.PHONE_EXISTED.getCode());
            response.setMessage(ErrorCode.PHONE_EXISTED.getMessage());
            response.setData(request.getPhone());
            exceptions.add(response);
        }
        if (!exceptions.isEmpty()) {
            throw new ArgumentNotValidException(exceptions);
        }

        String oldRoleName = user.getRole() != null ? user.getRole().getName() : null;
        boolean roleChanged = false;
        if (request.getRoleId() != null
                && (user.getRole() == null || user.getRole().getId() != request.getRoleId())) {
            if (userId.equals(actorId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCode.CANNOT_CHANGE_OWN_ROLE.name());
            }
            // Citizen accounts never become staff (and vice versa) via this API —
            // staff get their own accounts (spec decision #2 / RBAC §6).
            if (user.getRole() != null && RoleCodes.CITIZEN.equals(user.getRole().getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCode.CITIZEN_ROLE_RESTRICTED.name());
            }
            Role newRole = roleRepository.findById(request.getRoleId().intValue());
            if (newRole == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND.name());
            }
            if (RoleCodes.CITIZEN.equals(newRole.getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCode.CITIZEN_ROLE_RESTRICTED.name());
            }
            user.setRole(newRole);
            roleChanged = true;
        }

        if (!isBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (!isBlank(request.getName())) {
            user.setName(request.getName());
        }
        if (!isBlank(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (!isBlank(request.getPosition())) {
            user.setPosition(request.getPosition());
        }
        if (!isBlank(request.getAssignedArea())) {
            user.setAssignedArea(request.getAssignedArea());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        // Password is optional on update; never store it raw
        if (!isBlank(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdateAt(LocalDate.now());
        user.setUpdateBy(actorId);

        userRepository.save(user);

        if (roleChanged) {
            writeLog(Action.CHANGE_ROLE, user.getId(), actorId,
                    oldRoleName + " -> " + user.getRole().getName());
        } else {
            writeLog(Action.UPDATE_USER, user.getId(), actorId, null);
        }
        return AdminUserDetailResponse.from(user);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse changeStatus(String userId, UpdateUserStatusRequest request) {
        // Only the two admin-managed statuses are legal here; BANNED has no flow yet.
        AccountStatus target;
        if (AccountStatus.ACTIVE.name().equals(request.getStatus())) {
            target = AccountStatus.ACTIVE;
        } else if (AccountStatus.INACTIVE.name().equals(request.getStatus())) {
            target = AccountStatus.INACTIVE;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATUS.name());
        }

        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCode.USERNAME_NOT_EXISTED.name()));

        String actorId = securityUtil.getCurrentUserId();
        // An admin who deactivates themself has no way back in.
        if (userId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCode.CANNOT_DEACTIVATE_SELF.name());
        }
        if (user.getStatus() == target) {
            // Keep the 10026 business code but tell the FE the current state,
            // so it can't render "deactivated successfully" on an already-inactive account.
            APIResponse<String> response = new APIResponse<>();
            response.setCode(ErrorCode.INVALID_STATUS.getCode());
            response.setMessage(ErrorCode.INVALID_STATUS.getMessage()
                    + " (current status: " + target.name() + ")");
            throw new InvalidArgsException(response);
        }

        AccountStatus oldStatus = user.getStatus();
        user.setStatus(target);
        user.setUpdateAt(LocalDate.now());
        user.setUpdateBy(actorId);
        userRepository.save(user);

        Action action = target == AccountStatus.INACTIVE ? Action.USER_DEACTIVATE : Action.USER_ACTIVATE;
        writeLog(action, user.getId(), actorId,
                oldStatus != null ? oldStatus.name() : null, target.name());
        return AdminUserDetailResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(String id) {
        User user = userRepository.findWithRoleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCode.USERNAME_NOT_EXISTED.name()));
        return AdminUserDetailResponse.from(user);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void writeLog(Action action, String targetUserId, String actorId, String detail) {
        writeLog(action, targetUserId, actorId, null, detail);
    }

    private void writeLog(Action action, String targetUserId, String actorId,
                          String oldValue, String newValue) {
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(actorId)
                    .action(action.getAction())
                    .entityType(Table.USER.getTableName())
                    .entityId(targetUserId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(RequestIpUtil.getCurrentClientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write {} audit log: {}", action, e.getMessage());
        }
    }
}
