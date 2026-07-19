package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RbacEnforcementIT extends AbstractIntegrationTest {

    private static final String SUPPORT_REQUEST_BODY = """
            {
              "category": "TECHNICAL_ISSUE",
              "subject": "RBAC IT",
              "description": "integration test support request"
            }
            """;

    private int citizenRoleId() {
        return roleRepository.findByName("Citizen").orElseThrow().getId();
    }

    private Set<Integer> citizenGrants() {
        return permissionRepository.findRightIdsByRoleId(citizenRoleId());
    }

    private void putCitizenPermissions(Cookie adminToken, Set<Integer> rightIds) throws Exception {
        String body = "{\"rightIds\":" + rightIds.stream().sorted()
                .map(String::valueOf).collect(Collectors.joining(",", "[", "]")) + "}";
        mockMvc.perform(put("/admin/roles/" + citizenRoleId() + "/permissions")
                .cookie(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("AC1 - registration always yields Citizen with 19 base rights")
    class Registration {

        @Test
        void register_shouldAssignCitizenRole() throws Exception {
            MvcResult result = registerUser(uniqueEmail(), uniquePhone());
            String role = JsonPath.read(result.getResponse().getContentAsString(), "$.data.role");
            assertEquals("Citizen", role);
        }

        @Test
        void citizenBaseGrantSet_shouldContain19Rights() {
            assertEquals(19, citizenGrants().size());
        }

        @Test
        void register_shouldIgnoreInjectedRoleField() throws Exception {
            // RegisterRequest has no role field; unknown JSON keys are ignored
            String body = """
                    {
                      "fullName": "IT User",
                      "dateOfBirth": "01/01/2000",
                      "email": "%s",
                      "phone": "%s",
                      "password": "%s",
                      "passwordConfirmation": "%s",
                      "role": "Admin",
                      "roleId": 8
                    }
                    """.formatted(uniqueEmail(), uniquePhone(), DEFAULT_PASSWORD, DEFAULT_PASSWORD);
            MvcResult result = mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andReturn();
            assertEquals("Citizen",
                    JsonPath.read(result.getResponse().getContentAsString(), "$.data.role"));
        }
    }

    @Nested
    @DisplayName("AC3 - missing authority yields 403 plus ILLEGAL_REQUEST log")
    class ForbiddenAudit {

        @Test
        void citizen_callingOfficerEndpoint_shouldGet403AndBeLogged() throws Exception {
            MvcResult citizen = registerUser(uniqueEmail(), uniquePhone());
            Cookie token = cookieOf(citizen, "access_token");
            String citizenId = userIdFromBody(citizen);

            // Requires APPLICATION_INTAKE_CREATE, which Citizen never holds
            mockMvc.perform(post("/application/receipt?status=draft")
                    .cookie(token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(10020));

            boolean audited = systemLogRepository.findAll().stream()
                    .anyMatch(log -> "ILLEGAL_REQUEST".equals(log.getAction())
                            && citizenId.equals(log.getUserId()));
            assertTrue(audited);
        }
    }

    @Nested
    @DisplayName("AC4 - permission changes apply immediately, no re-login")
    class ImmediateEffect {

        @Test
        void untickThenRetick_shouldToggle403WithSameToken() throws Exception {
            MvcResult citizen = registerUser(uniqueEmail(), uniquePhone());
            Cookie citizenToken = cookieOf(citizen, "access_token");
            MvcResult admin = registerUserWithRole("Admin");
            Cookie adminToken = cookieOf(admin, "access_token");

            Set<Integer> original = citizenGrants();
            int supportRequestCreate = rightRepository.findByCode("SUPPORT_REQUEST_CREATE")
                    .orElseThrow().getId();

            try {
                mockMvc.perform(post("/support-requests").cookie(citizenToken)
                        .contentType(MediaType.APPLICATION_JSON).content(SUPPORT_REQUEST_BODY))
                        .andExpect(status().isOk());

                Set<Integer> without = original.stream()
                        .filter(id -> id != supportRequestCreate).collect(Collectors.toSet());
                putCitizenPermissions(adminToken, without);

                // Same access token, next request: 403
                mockMvc.perform(post("/support-requests").cookie(citizenToken)
                        .contentType(MediaType.APPLICATION_JSON).content(SUPPORT_REQUEST_BODY))
                        .andExpect(status().isForbidden());

                putCitizenPermissions(adminToken, original);

                mockMvc.perform(post("/support-requests").cookie(citizenToken)
                        .contentType(MediaType.APPLICATION_JSON).content(SUPPORT_REQUEST_BODY))
                        .andExpect(status().isOk());
            } finally {
                // Never leave the shared Citizen role mutated for other tests
                putCitizenPermissions(adminToken, original);
            }
        }
    }

    @Nested
    @DisplayName("AC10 - deny-by-default returns 401 without token")
    class DenyByDefault {

        @Test
        void protectedEndpoints_withoutToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/applications")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/admin/rights")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/form-type")).andExpect(status().isUnauthorized());
        }

        @Test
        void publicEndpoints_shouldStayReachable() throws Exception {
            mockMvc.perform(get("/policy")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("AC11 - APPLICATION_VIEW_OWN cannot read someone else's data")
    class Ownership {

        @Test
        void citizenA_listingCitizenBApplications_shouldGet403() throws Exception {
            MvcResult citizenA = registerUser(uniqueEmail(), uniquePhone());
            MvcResult citizenB = registerUser(uniqueEmail(), uniquePhone());
            Cookie tokenA = cookieOf(citizenA, "access_token");
            String idB = userIdFromBody(citizenB);

            mockMvc.perform(get("/applications/submit-by/" + idB).cookie(tokenA))
                    .andExpect(status().isForbidden());
        }

        @Test
        void citizenA_listingOwnApplications_shouldSucceed() throws Exception {
            MvcResult citizenA = registerUser(uniqueEmail(), uniquePhone());
            Cookie tokenA = cookieOf(citizenA, "access_token");
            String idA = userIdFromBody(citizenA);

            mockMvc.perform(get("/applications/submit-by/" + idA).cookie(tokenA))
                    .andExpect(status().isOk());
        }

        @Test
        void officerWithFullView_shouldReadAnySubmitter() throws Exception {
            MvcResult reception = registerUserWithRole("Reception");
            Cookie token = cookieOf(reception, "access_token");
            MvcResult citizen = registerUser(uniqueEmail(), uniquePhone());
            String citizenId = userIdFromBody(citizen);

            mockMvc.perform(get("/applications/submit-by/" + citizenId).cookie(token))
                    .andExpect(status().isOk());
        }
    }
}
