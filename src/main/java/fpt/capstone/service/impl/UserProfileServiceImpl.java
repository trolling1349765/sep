package fpt.capstone.service.impl;

import fpt.capstone.dto.request.UpdateProfileRequest;
import fpt.capstone.dto.response.UserProfileResponse;
import fpt.capstone.entity.User;
import fpt.capstone.enums.RoleName;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private static final String CITIZEN_ROLE = RoleName.CONG_DAN.name();

    private final UserRepository userRepository;

    @Override
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        return UserProfileResponse.fromUser(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }

        // Only users with role "Công dân" can update their profile
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if (!CITIZEN_ROLE.equals(roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only citizens (Công dân) can update their profile.");
        }

        // Update name
        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }

        // Update national ID
        if (request.getNationalId() != null) {
            user.setNationalId(request.getNationalId().trim());
        }

        // Update date of birth
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }

        // Update email
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().toLowerCase());
        }

        // Update gender
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        // Update phone
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        // Update address fields
        if (request.getProvinceCode() != null) {
            user.setProvinceCode(request.getProvinceCode().trim());
        }
        if (request.getProvinceName() != null) {
            user.setProvinceName(request.getProvinceName().trim());
        }
        if (request.getWardCode() != null) {
            user.setWardCode(request.getWardCode().trim());
        }
        if (request.getWardName() != null) {
            user.setWardName(request.getWardName().trim());
        }
        if (request.getSpecificAddress() != null) {
            user.setSpecificAddress(request.getSpecificAddress().trim());
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());

        return UserProfileResponse.fromUser(user);
    }
}