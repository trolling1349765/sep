package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.User;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public User Login(String username, String password) {
        User user = userRepository.getUsersByUsername(username);
        if (user == null) return null;
        if (user.getPassword().equals(password)) return user;
        return null;
    }

    @Override
    public User LoginWithEmail(String email, String password) {
        User user = userRepository.findUserByEmail(email);
        if (user == null) return null;
        if (user.getPassword().equals(password)) return user;
        return null;
    }

    @Override
    public Boolean register(User user) {
        userRepository.save(user);
        return true;
    }

    @Override
    public User createRequest(UserCreationRequest request) {
        User user = new User();
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
        user.setId(request.getUserId());
        user.setUsername(request.getUsername());
        user.setDob(request.getDob());
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(request.getPassword());

        if (!exceptions.isEmpty()) {
            throw new AppException(exceptions);
        }
        return userRepository.save(user);
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(()-> new RuntimeException("user not found"));
    }
}
