package fpt.capstone.service;

import fpt.capstone.entity.User;

public interface UserService {

    User Login(String username, String password);

    User LoginWithEmail(String email, String password);

    Boolean register(User user);


}
