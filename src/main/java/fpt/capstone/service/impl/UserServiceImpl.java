package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createRequest(UserCreationRequest request, String userId) {

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

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        User user = new User();
        user.builder()
                .role(request.getRole())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .dob(request.getDob())
                .createAt(LocalDate.now())
                .createBy(userId)
                .build();

        if (!exceptions.isEmpty()) {
            throw new ArgumentNotValidException(exceptions);
        }
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UserUpdateRequest request) {
        User user = userRepository.getUserById(request.getUserId());
        user.setEmail(request.getEmail());
        user.setDob(request.getDob());
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        return userRepository.save(user);
    }

    @Override
    public List<UserResponse> getUsers() {
        List<UserResponse> userResponseList = new ArrayList<>();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            UserResponse userResponse = new UserResponse(user);
            userResponseList.add(userResponse);
        }
        return userResponseList;
    }

    @Override
    public UserResponse getUser(String id) {
        User user = userRepository.getUserById(id);
        return new UserResponse(user);
    }
}
