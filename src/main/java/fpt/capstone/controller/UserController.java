package fpt.capstone.controller;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.User;
import fpt.capstone.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    APIResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        APIResponse<User> response = new APIResponse<>();
        response.setCode(200);
        response.setMessage("user created");
        response.setData(userService.createRequest(request));
        return response;
    }

    @GetMapping("/user")
    List<User> getUser() {
        return userService.getUsers();
    }

    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }
}
