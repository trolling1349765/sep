package fpt.capstone.service.impl;

import fpt.capstone.entity.User;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserService;
import org.springframework.stereotype.Service;

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
}
