package fpt.capstone.service;

import fpt.capstone.dto.response.RolePermissionsResponse;
import fpt.capstone.dto.response.RoleSummaryResponse;

import java.util.List;

public interface PermissionService {

    List<RoleSummaryResponse> getRoles();

    RolePermissionsResponse getRolePermissions(int roleId);

    /**
     * Replace-set semantics: the given rightIds become the role's exact grant set.
     * Applies guard rules (Admin role locked, system rights irremovable), writes a
     * PERMISSION_UPDATE audit row with the diff, all in one transaction.
     */
    RolePermissionsResponse updateRolePermissions(int roleId, List<Integer> rightIds);
}
