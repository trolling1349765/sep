package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.service.UserService;
import fpt.capstone.util.RequestIpUtil;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final SystemLogService systemLogService;

    @Override
    @Transactional
    public UserResponse createRequest(UserCreationRequest request) {

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
        if (!exceptions.isEmpty()) {
            throw new ArgumentNotValidException(exceptions);
        }

        Role role = roleRepository.findById(request.getRole());
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND.name());
        }

        String actorId = securityUtil.getCurrentUserId();
        User user = User.builder()
                .role(role)
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .dob(request.getDob())
                .createAt(LocalDate.now())
                .createBy(actorId != null ? actorId : "")
                .build();

        userRepository.save(user);
        writeLog(Action.CREATE_USER, user.getId(), actorId, role.getName());
        return new UserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.USERNAME_NOT_EXISTED.name());
        }

        String actorId = securityUtil.getCurrentUserId();

        String oldRoleName = user.getRole() != null ? user.getRole().getName() : null;
        boolean roleChanged = false;
        if (request.getRoleId() != null
                && (user.getRole() == null || user.getRole().getId() != request.getRoleId())) {
            if (userId.equals(actorId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCode.CANNOT_CHANGE_OWN_ROLE.name());
            }
            Role newRole = roleRepository.findById(request.getRoleId().intValue());
            if (newRole == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND.name());
            }
            user.setRole(newRole);
            roleChanged = true;
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        // Password is optional on update; never store it raw
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
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
        return new UserResponse(user);
    }

    @Override
    public List<UserResponse> getUsers() {
        List<UserResponse> userResponseList = new ArrayList<>();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userResponseList.add(new UserResponse(user));
        }
        return userResponseList;
    }

    @Override
    public UserResponse getUser(String id) {
        User user = userRepository.getUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.USERNAME_NOT_EXISTED.name());
        }
        return new UserResponse(user);
    }

    private void writeLog(Action action, String targetUserId, String actorId, String detail) {
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(actorId)
                    .action(action.getAction())
                    .entityType(Table.USER.getTableName())
                    .entityId(targetUserId)
                    .newValue(detail)
                    .ipAddress(RequestIpUtil.getCurrentClientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write {} audit log: {}", action, e.getMessage());
        }
    }
}
