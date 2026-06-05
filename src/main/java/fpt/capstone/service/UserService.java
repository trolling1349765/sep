package fpt.capstone.service;

import fpt.capstone.entity.User;

public interface UserService {

    User findByUsername(String username);

    User findByEmail(String email);


}
