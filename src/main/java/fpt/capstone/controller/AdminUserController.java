package fpt.capstone.controller;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<APIResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(APIResponse.success(userService.getUsers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<APIResponse<UserResponse>> getUser(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(userService.getUser(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<APIResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity.ok(APIResponse.success("User created successfully.",
                userService.createRequest(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<APIResponse<UserResponse>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(APIResponse.success("User updated successfully.",
                userService.updateUser(id, request)));
    }
}
