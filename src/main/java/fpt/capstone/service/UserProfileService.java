package fpt.capstone.service;

import fpt.capstone.dto.request.UpdateProfileRequest;
import fpt.capstone.dto.response.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(String userId);

    UserProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}