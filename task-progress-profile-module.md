# User Profile Module Implementation Plan

## Backend (Spring Boot)
- [ ] 1. Add `phone`, `address`, `avatarUrl`, `status` fields to User entity
- [ ] 2. Create `UserProfileResponse` DTO
- [ ] 3. Create `UpdateProfileRequest` DTO (with phone validation)
- [ ] 4. Add `confirmNewPassword` to `ChangePasswordRequest` DTO
- [ ] 5. Create `PasswordChangeRateLimiterService` (per-user rate limiting)
- [ ] 6. Create `UserProfileService` interface and `UserProfileServiceImpl`
- [ ] 7. Update `AuthServiceImpl.changePassword()` to accept confirmNewPassword validation
- [ ] 8. Create `UserProfileController` (GET/PUT /users/profile)
- [ ] 9. Update `SecurityConfig` to secure `/users/**` endpoints

## Frontend (React)
- [ ] 10. Add profile API methods to `AuthContext`
- [ ] 11. Create `ProfilePage.jsx` (read-only view with avatar, skeleton loading)
- [ ] 12. Create `UpdateProfilePage.jsx` (edit form with dirty checking, validation)
- [ ] 13. Create `ChangePasswordPage.jsx` (3 fields, strength meter, eye toggle)
- [ ] 14. Update `App.jsx` with new routes
- [ ] 15. Update `application.properties` with password rate limit config