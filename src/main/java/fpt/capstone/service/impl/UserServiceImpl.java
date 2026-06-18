package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.mapper.UserMapper;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,  UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public User createRequest(UserCreationRequest request) {

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

        User user = userMapper.toUser(request);

        if (!exceptions.isEmpty()) {
            throw new ArgumentNotValidException(exceptions);
        }
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UserUpdateRequest request) {
        User user = userRepository.getUserById(request.getUserId());
        userMapper.updateUser(user, request);
        return userRepository.save(user);
    }

    @Override
    public List<UserResponse> getUsers() {
        List<UserResponse> userResponseList = new ArrayList<>();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userResponseList.add(userMapper.toUserResponse(user));
        }
        return userResponseList;
    }

    @Override
    public UserResponse getUser(String id) {
        User user = userRepository.getUserById(id);
        return userMapper.toUserResponse(user);
    }
}
