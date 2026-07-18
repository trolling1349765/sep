package fpt.capstone.controller;

import fpt.capstone.dto.request.CreateRightRequest;
import fpt.capstone.dto.request.UpdateRightRequest;
import fpt.capstone.dto.request.UpdateRolePermissionsRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.RightModuleResponse;
import fpt.capstone.dto.response.RightResponse;
import fpt.capstone.dto.response.RolePermissionsResponse;
import fpt.capstone.dto.response.RoleSummaryResponse;
import fpt.capstone.service.PermissionCacheService;
import fpt.capstone.service.PermissionService;
import fpt.capstone.service.RightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;
    private final RightService rightService;
    private final PermissionCacheService permissionCacheService;

    @GetMapping("/rights")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<APIResponse<List<RightModuleResponse>>> getRights() {
        return ResponseEntity.ok(APIResponse.success(rightService.getCatalogue()));
    }

    @PostMapping("/rights")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<APIResponse<RightResponse>> createRight(
            @Valid @RequestBody CreateRightRequest request) {
        RightResponse response = rightService.createRight(request);
        // Evict after the transaction has committed. A new right belongs
        // to no role yet, but the spec mandates eviction on every catalogue write.
        permissionCacheService.evictAll();
        return ResponseEntity.ok(APIResponse.success("Right created successfully.", response));
    }

    @PutMapping("/rights/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<APIResponse<RightResponse>> updateRight(
            @PathVariable int id,
            @Valid @RequestBody UpdateRightRequest request) {
        return ResponseEntity.ok(APIResponse.success(rightService.updateRight(id, request)));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('PERMISSION_MANAGE', 'ROLE_MANAGE')")
    public ResponseEntity<APIResponse<List<RoleSummaryResponse>>> getRoles() {
        return ResponseEntity.ok(APIResponse.success(permissionService.getRoles()));
    }

    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<APIResponse<RolePermissionsResponse>> getRolePermissions(
            @PathVariable int roleId) {
        return ResponseEntity.ok(APIResponse.success(permissionService.getRolePermissions(roleId)));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<APIResponse<RolePermissionsResponse>> updateRolePermissions(
            @PathVariable int roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        RolePermissionsResponse response = permissionService.updateRolePermissions(roleId, request.getRightIds());
        permissionCacheService.evictAll();
        return ResponseEntity.ok(APIResponse.success("Permissions updated successfully.", response));
    }
}
