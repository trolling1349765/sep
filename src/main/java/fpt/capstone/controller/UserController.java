package fpt.capstone.controller;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;
import fpt.capstone.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    APIResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        UserResponse userResponse = userService.createRequest(request);
        APIResponse<UserResponse> response = new APIResponse<>();
        response.setCode(200);
        response.setMessage("user created");
        response.setData(userResponse);
        return response;
    }

    @GetMapping()
    List<UserResponse> getUser() {
        return userService.getUsers();
    }

}
