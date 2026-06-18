package fpt.capstone.service;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface UserService {

    User createRequest(UserCreationRequest request);

    List<UserResponse> getUsers();

    UserResponse getUser(String id) throws Throwable;

    User updateUser(UserUpdateRequest request);
}
