package fpt.capstone.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserProfileFlowIT extends AbstractIntegrationTest {

        // ============================================================
        // GET /users/profile
        // ============================================================
        @Nested
        class GetProfileTests {

                @Test
                void getProfile_withAccessCookie_returnsProfile() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        String userId = userIdFromBody(registered);

                        // Act & Assert
                        mockMvc.perform(get("/users/profile").cookie(cookieOf(registered, "access_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                                        .andExpect(jsonPath("$.data.userId").value(userId))
                                        .andExpect(jsonPath("$.data.email").value(email))
                                        .andExpect(jsonPath("$.data.name").value("IT User"))
                                        .andExpect(jsonPath("$.data.role").value("Citizen"));
                }

                @Test
                void getProfile_withBearerToken_returnsProfile() throws Exception {
                        // Arrange - same JWT, but via Authorization header:
                        // /users/* accepts Bearer fallback (asymmetric with cookie-only /auth/*)
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        String token = cookieOf(registered, "access_token").getValue();

                        // Act & Assert
                        mockMvc.perform(get("/users/profile").header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.email").value(email));
                }

                @Test
                void getProfile_withoutAuth_returns401() throws Exception {
                        mockMvc.perform(get("/users/profile"))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.code").value(401))
                                        .andExpect(jsonPath("$.message").value("Authentication required."));
                }

                @Test
                void getProfile_withGarbageToken_returns401() throws Exception {
                        mockMvc.perform(get("/users/profile").cookie(new Cookie("access_token", "garbage")))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message").value("Authentication required."));
                }
        }

        // ============================================================
        // PUT /users/profile
        // ============================================================
        @Nested
        class UpdateProfileTests {

                @Test
                void updateProfile_partialUpdate_updatesOnlyProvidedFields() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        Cookie accessCookie = cookieOf(registered, "access_token");
                        String newPhone = uniquePhone();

                        // Act - only phone + gender are sent
                        mockMvc.perform(put("/users/profile")
                                        .cookie(accessCookie)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"phone\": \"%s\", \"gender\": true}".formatted(newPhone)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                                        .andExpect(jsonPath("$.data.phone").value(newPhone))
                                        .andExpect(jsonPath("$.data.gender").value(true))
                                        .andExpect(jsonPath("$.data.name").value("IT User"))
                                        .andExpect(jsonPath("$.data.email").value(email));

                        // Assert - second GET proves the change persisted and nothing else moved
                        mockMvc.perform(get("/users/profile").cookie(accessCookie))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.phone").value(newPhone))
                                        .andExpect(jsonPath("$.data.name").value("IT User"))
                                        .andExpect(jsonPath("$.data.email").value(email));
                }

                @Test
                void updateProfile_fullUpdate_persistsAllFieldsAndFullAddress() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        Cookie accessCookie = cookieOf(registered, "access_token");
                        String newEmail = uniqueEmail();
                        String newPhone = uniquePhone();
                        String nationalId = uniqueNationalId();
                        String body = """
                                        {
                                          "name": "Updated Name",
                                          "nationalId": "%s",
                                          "dob": "1995-05-20",
                                          "email": "%s",
                                          "phone": "%s",
                                          "gender": false,
                                          "provinceCode": "01",
                                          "provinceName": "Ha Noi",
                                          "wardCode": "00001",
                                          "wardName": "Phuc Xa",
                                          "specificAddress": "123 Duong Lang"
                                        }
                                        """.formatted(nationalId, newEmail, newPhone);

                        // Act
                        mockMvc.perform(put("/users/profile")
                                        .cookie(accessCookie)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.name").value("Updated Name"))
                                        .andExpect(jsonPath("$.data.nationalId").value(nationalId))
                                        .andExpect(jsonPath("$.data.dob").value("1995-05-20"))
                                        .andExpect(jsonPath("$.data.email").value(newEmail))
                                        .andExpect(jsonPath("$.data.phone").value(newPhone))
                                        .andExpect(jsonPath("$.data.fullAddress")
                                                        .value("123 Duong Lang, Phuc Xa, Ha Noi"));

                        // Assert - persisted (fresh read through the API)
                        mockMvc.perform(get("/users/profile").cookie(accessCookie))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.name").value("Updated Name"))
                                        .andExpect(jsonPath("$.data.dob").value("1995-05-20"))
                                        .andExpect(jsonPath("$.data.email").value(newEmail))
                                        .andExpect(jsonPath("$.data.fullAddress")
                                                        .value("123 Duong Lang, Phuc Xa, Ha Noi"));
                }

                @Test
                void updateProfile_invalidPhone_returns400() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        String phone = uniquePhone();
                        MvcResult registered = registerUser(email, phone);
                        Cookie accessCookie = cookieOf(registered, "access_token");

                        // Act
                        mockMvc.perform(put("/users/profile")
                                        .cookie(accessCookie)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"phone\": \"123\"}"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(400))
                                        .andExpect(jsonPath("$.message").value(
                                                        "Phone number must be 10 digits starting with 0 or +84."))
                                        .andExpect(jsonPath("$.data.phone").value("123"));

                        // Assert - nothing changed
                        mockMvc.perform(get("/users/profile").cookie(accessCookie))
                                        .andExpect(jsonPath("$.data.phone").value(phone));
                }

                @Test
                void updateProfile_invalidEmail_returns400() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());

                        // Act & Assert
                        mockMvc.perform(put("/users/profile")
                                        .cookie(cookieOf(registered, "access_token"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\": \"bad\"}"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("Email must be valid."));
                }

                @Test
                void updateProfile_withoutAuth_returns401() throws Exception {
                        mockMvc.perform(put("/users/profile")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\": \"Nobody\"}"))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message").value("Authentication required."));
                }
        }
}
