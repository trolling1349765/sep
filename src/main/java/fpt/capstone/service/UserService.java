package fpt.capstone.service;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getUsers();

    UserResponse getUser(String id);

    /** Admin creates a staff account with an explicit role (spec §6). */
    UserResponse createRequest(UserCreationRequest request);

    UserResponse updateUser(String userId, UserUpdateRequest request);
}
