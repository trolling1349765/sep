package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminRbacApiIT extends AbstractIntegrationTest {

        private Cookie adminToken() throws Exception {
                return cookieOf(registerUserWithRole("Admin"), "access_token");
        }

        @Nested
        @DisplayName("AC2 - admin creates staff accounts with a role")
        class StaffAccounts {

                @Test
                void createReceptionUser_shouldLoginAndCarryReceptionRights() throws Exception {
                        Cookie admin = adminToken();
                        Role reception = roleRepository.findByName("Reception").orElseThrow();
                        String email = uniqueEmail();
                        String body = """
                                        {
                                          "role": %d,
                                          "name": "Reception Staff",
                                          "email": "%s",
                                          "username": "%s",
                                          "password": "%s",
                                          "dob": "2000-01-01"
                                        }
                                        """.formatted(reception.getId(), email, email, DEFAULT_PASSWORD);

                        MvcResult created = mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isOk())
                                        .andReturn();
                        assertEquals("Reception",
                                        JsonPath.read(created.getResponse().getContentAsString(), "$.data.role"));
                        assertEquals(30, permissionRepository.countByRoleId(reception.getId()));

                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(200, login.getResponse().getStatus());
                        Cookie staffToken = cookieOf(login, "access_token");

                        // Reception holds APPLICATION_VIEW but no PERMISSION_MANAGE
                        mockMvc.perform(get("/applications").cookie(staffToken)).andExpect(status().isOk());
                        mockMvc.perform(get("/admin/rights").cookie(staffToken)).andExpect(status().isForbidden());
                }

                @Test
                void createUser_withUnknownRole_shouldReturn400() throws Exception {
                        Cookie admin = adminToken();
                        String email = uniqueEmail();
                        String body = """
                                        { "role": 9999, "name": "x", "email": "%s", "username": "%s", "password": "%s" }
                                        """.formatted(email, email, DEFAULT_PASSWORD);

                        mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("ROLE_NOT_FOUND"));
                }
        }

        @Nested
        @DisplayName("AC6 - rights are append-only")
        class RightCreation {

                @Test
                void createRight_thenDuplicate_shouldReturn200Then409() throws Exception {
                        Cookie admin = adminToken();
                        String code = "IT_RIGHT_" + uniq();
                        String body = """
                                        { "code": "%s", "name": "it right", "module": "BAO_CAO" }
                                        """.formatted(code);

                        mockMvc.perform(post("/admin/rights").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.isSystem").value(false));

                        // Appears in the catalogue, assigned to no role
                        MvcResult catalogue = mockMvc.perform(get("/admin/rights").cookie(admin))
                                        .andExpect(status().isOk()).andReturn();
                        assertTrue(catalogue.getResponse().getContentAsString().contains(code));
                        int newRightId = rightRepository.findByCode(code).orElseThrow().getId();
                        roleRepository.findAll().forEach(role -> assertFalse(
                                        permissionRepository.findRightIdsByRoleId(role.getId()).contains(newRightId),
                                        "New right must not be pre-assigned to role " + role.getName()));

                        mockMvc.perform(post("/admin/rights").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.message").value("RIGHT_CODE_EXISTS"));
                }

                @Test
                void createRight_withInvalidCode_shouldReturn400() throws Exception {
                        Cookie admin = adminToken();
                        mockMvc.perform(post("/admin/rights").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"code\": \"bad-code\", \"name\": \"x\", \"module\": \"BAO_CAO\" }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("INVALID_RIGHT_CODE"));
                }

                @Test
                void deleteRight_endpointMustNotExist() throws Exception {
                        Cookie admin = adminToken();
                        int anyRightId = rightRepository.findByCode("POLICY_VIEW").orElseThrow().getId();
                        mockMvc.perform(delete("/admin/rights/" + anyRightId).cookie(admin))
                                        .andExpect(status().isMethodNotAllowed());
                }
        }

        @Nested
        @DisplayName("AC7/AC8 - replace-set validation, rollback and guard rules")
        class PermissionGuards {

                private int citizenRoleId() {
                        return roleRepository.findByName("Citizen").orElseThrow().getId();
                }

                @Test
                void putWithUnknownRightId_shouldReturn400AndChangeNothing() throws Exception {
                        Cookie admin = adminToken();
                        Set<Integer> before = permissionRepository.findRightIdsByRoleId(citizenRoleId());

                        mockMvc.perform(put("/admin/roles/" + citizenRoleId() + "/permissions").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"rightIds\": [999999] }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("RIGHT_NOT_FOUND"));

                        assertEquals(before, new HashSet<>(permissionRepository.findRightIdsByRoleId(citizenRoleId())));
                }

                @Test
                void putRemovingSystemRight_shouldReturn400SystemRightRequired() throws Exception {
                        Cookie admin = adminToken();
                        int profileView = rightRepository.findByCode("PROFILE_VIEW").orElseThrow().getId();
                        Set<Integer> withoutSystem = new HashSet<>(
                                        permissionRepository.findRightIdsByRoleId(citizenRoleId()));
                        withoutSystem.remove(profileView);
                        String body = "{\"rightIds\":" + withoutSystem.toString() + "}";

                        mockMvc.perform(put("/admin/roles/" + citizenRoleId() + "/permissions").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("SYSTEM_RIGHT_REQUIRED"));
                }

                @Test
                void putOnAdminRole_shouldReturn403AdminRoleLocked() throws Exception {
                        Cookie admin = adminToken();
                        int adminRoleId = roleRepository.findByName("Admin").orElseThrow().getId();

                        mockMvc.perform(put("/admin/roles/" + adminRoleId + "/permissions").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content("{ \"rightIds\": [1] }"))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.message").value("ADMIN_ROLE_LOCKED"));
                }

                @Test
                void putOnUnknownRole_shouldReturn404() throws Exception {
                        Cookie admin = adminToken();
                        mockMvc.perform(put("/admin/roles/999/permissions").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON).content("{ \"rightIds\": [1] }"))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.message").value("ROLE_NOT_FOUND"));
                }
        }

        @Nested
        @DisplayName("AC9 - every admin write leaves an audit row")
        class AuditRows {

                @Test
                void adminWrites_shouldProduceAuditLogs() throws Exception {
                        Cookie admin = adminToken();
                        String code = "IT_AUDIT_RIGHT_" + uniq();
                        mockMvc.perform(post("/admin/rights").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"code\": \"%s\", \"name\": \"x\", \"module\": \"BAO_CAO\" }"
                                                        .formatted(code)))
                                        .andExpect(status().isOk());

                        assertTrue(systemLogRepository.findAll().stream()
                                        .anyMatch(log -> "RIGHT_CREATE".equals(log.getAction())
                                                        && String.valueOf(log.getNewValue()).contains(code)));
                }

                @Test
                void permissionPut_shouldLogDiff() throws Exception {
                        Cookie admin = adminToken();
                        int citizenRoleId = roleRepository.findByName("Citizen").orElseThrow().getId();
                        Set<Integer> original = permissionRepository.findRightIdsByRoleId(citizenRoleId);
                        int chatbotUse = rightRepository.findByCode("CHATBOT_USE").orElseThrow().getId();
                        Set<Integer> without = new HashSet<>(original);
                        without.remove(chatbotUse);

                        try {
                                mockMvc.perform(put("/admin/roles/" + citizenRoleId + "/permissions").cookie(admin)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"rightIds\":" + without + "}"))
                                                .andExpect(status().isOk());

                                assertTrue(systemLogRepository.findAll().stream()
                                                .anyMatch(log -> "PERMISSION_UPDATE".equals(log.getAction())
                                                                && String.valueOf(log.getNewValue())
                                                                                .contains("CHATBOT_USE")));
                        } finally {
                                mockMvc.perform(put("/admin/roles/" + citizenRoleId + "/permissions").cookie(admin)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"rightIds\":" + original + "}"))
                                                .andExpect(status().isOk());
                        }
                }
        }

        @Nested
        @DisplayName("AC14 - admin cannot change their own role")
        class OwnRoleGuard {

                @Test
                void changingOwnRole_shouldReturn400() throws Exception {
                        MvcResult admin = registerUserWithRole("Admin");
                        Cookie token = cookieOf(admin, "access_token");
                        String ownId = userIdFromBody(admin);
                        int receptionId = roleRepository.findByName("Reception").orElseThrow().getId();

                        mockMvc.perform(put("/admin/users/" + ownId).cookie(token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"roleId\": " + receptionId + " }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("CANNOT_CHANGE_OWN_ROLE"));
                }

                @Test
                void changingAnotherAccountsRole_shouldSucceedAndLogChangeRole() throws Exception {
                        Cookie admin = adminToken();
                        MvcResult citizen = registerUser(uniqueEmail(), uniquePhone());
                        String citizenId = userIdFromBody(citizen);
                        int receptionId = roleRepository.findByName("Reception").orElseThrow().getId();

                        mockMvc.perform(put("/admin/users/" + citizenId).cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"roleId\": " + receptionId + " }"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.role").value("Reception"));

                        assertTrue(systemLogRepository.findAll().stream()
                                        .anyMatch(log -> "CHANGE_ROLE".equals(log.getAction())
                                                        && citizenId.equals(log.getEntityId())));
                }
        }
}
