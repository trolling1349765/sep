package fpt.capstone.controller;

import fpt.capstone.dto.request.UpdateUserStatusRequest;
import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AdminUserDetailResponse;
import fpt.capstone.dto.response.AdminUserListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.AccountStatus;
import fpt.capstone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin account management: staff accounts are only created here —
 * self-registration always yields the Citizen role.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<AdminUserListResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                userService.searchUsers(page, size, q, roleId, status, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<APIResponse<AdminUserDetailResponse>> getUser(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(userService.getUser(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<APIResponse<AdminUserDetailResponse>> createUser(
            @Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity.ok(APIResponse.success("User created successfully.",
                userService.createRequest(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<APIResponse<AdminUserDetailResponse>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(APIResponse.success("User updated successfully.",
                userService.updateUser(id, request)));
    }

    // Deactivation does not revoke live access tokens (stateless JWT, short TTL);
    // refresh is blocked in AuthServiceImpl, so lockout completes within the TTL.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public ResponseEntity<APIResponse<AdminUserDetailResponse>> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        AdminUserDetailResponse result = userService.changeStatus(id, request);
        String message = result.getStatus() == AccountStatus.INACTIVE
                ? "User deactivated. Existing access tokens expire naturally; refresh is blocked."
                : "User reactivated successfully.";
        return ResponseEntity.ok(APIResponse.success(message, result));
    }
}
