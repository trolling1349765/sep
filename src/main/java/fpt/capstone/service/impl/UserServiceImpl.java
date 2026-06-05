package fpt.capstone.service.impl;

import fpt.capstone.entity.User;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserService;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User findByUsername(String username) {
        return null;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }
}
