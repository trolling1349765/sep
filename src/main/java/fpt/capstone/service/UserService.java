package fpt.capstone.service;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;

import java.util.List;

public interface UserService {


    List<UserResponse> getUsers();

    UserResponse getUser(String id) throws Throwable;

    User createRequest(UserCreationRequest request, String userId);

    User updateUser(UserUpdateRequest request);
}
