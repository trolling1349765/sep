package fpt.capstone.service;

import fpt.capstone.dto.request.UpdateUserStatusRequest;
import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.AdminUserDetailResponse;
import fpt.capstone.dto.response.AdminUserListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.AccountStatus;

public interface UserService {

    /** Admin user list: search (username/name/email), filters, whitelisted sort, paging. */
    PageResponse<AdminUserListResponse> searchUsers(int page, int size, String q,
                                                    Integer roleId, AccountStatus status,
                                                    String sort, String dir);

    AdminUserDetailResponse getUser(String id);

    /** Admin creates a staff account with an explicit role (spec §6) — never Citizen. */
    AdminUserDetailResponse createRequest(UserCreationRequest request);

    AdminUserDetailResponse updateUser(String userId, UserUpdateRequest request);

    /** INACTIVE <-> ACTIVE only; guarded against self-deactivation and no-op changes. */
    AdminUserDetailResponse changeStatus(String userId, UpdateUserStatusRequest request);
}
