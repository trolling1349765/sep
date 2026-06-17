package fpt.capstone.service;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.entity.User;

import java.util.ArrayList;
import java.util.List;

public interface UserService {

    User Login(String username, String password);

    User LoginWithEmail(String email, String password);

    Boolean register(User user);

    User createRequest(UserCreationRequest request);

    List<User> getUsers();

    User getUser(Long id);

    User updateUser(UserUpdateRequest request);
}
