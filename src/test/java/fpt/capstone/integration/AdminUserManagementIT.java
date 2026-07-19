package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.config.RoleCodes;
import fpt.capstone.entity.RefreshToken;
import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import jakarta.servlet.http.Cookie;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin user-management screen (spec Quan ly nguoi dung v1.2): list/search,
 * staff creation, update, INACTIVE<->ACTIVE toggle, and the view-only Roles
 * screen. Inherits the exact bean-override set of AbstractIntegrationTest —
 * do NOT add @MockitoBean/@TestPropertySource here (context-cache key).
 */
class AdminUserManagementIT extends AbstractIntegrationTest {

        private Cookie adminToken() throws Exception {
                return cookieOf(registerUserWithRole("Admin"), "access_token");
        }

        private String staffBody(String email, String username, String phone, Integer roleId,
                        String assignedArea) {
                String area = assignedArea == null ? "" : "\"assignedArea\": \"" + assignedArea + "\",";
                return """
                                {
                                  "roleId": %d,
                                  "name": "IT Staff",
                                  "email": "%s",
                                  "username": "%s",
                                  "phone": "%s",
                                  "password": "%s",
                                  "position": "Can bo IT",
                                  %s
                                  "dob": "1995-01-01"
                                }
                                """.formatted(roleId, email, username, phone, DEFAULT_PASSWORD, area);
        }

        private int roleId(String name) {
                return roleRepository.findByName(name).orElseThrow().getId();
        }

        /** Creates a staff account via the API and returns its id. */
        private String createStaff(Cookie admin, String email, String username, String phone)
                        throws Exception {
                MvcResult created = mockMvc.perform(post("/admin/users").cookie(admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(staffBody(email, username, phone, roleId("Reception"), null)))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        }

        @Nested
        @DisplayName("AC-List - search, sort, paging")
        class ListAndSearch {

                @Test
                void list_shouldReturnPageShapeAndFindByUsernameNameEmail() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "srch" + uniq();
                        createStaff(admin, marker + "@example.com", marker, uniquePhone());

                        // exact username, case-insensitive, matched by q
                        MvcResult byUsername = mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", marker.toUpperCase()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.totalElements").value(1))
                                        .andExpect(jsonPath("$.data.page").value(0))
                                        .andExpect(jsonPath("$.data.size").value(20))
                                        .andExpect(jsonPath("$.data.totalPages").value(1))
                                        .andReturn();
                        String body = byUsername.getResponse().getContentAsString();
                        assertEquals(marker, JsonPath.read(body, "$.data.items[0].username"));
                        assertEquals("IT Staff", JsonPath.read(body, "$.data.items[0].name"));
                        assertEquals("Reception", JsonPath.read(body, "$.data.items[0].roleName"));
                        assertEquals("ACTIVE", JsonPath.read(body, "$.data.items[0].status"));
                        assertEquals(false, JsonPath.read(body, "$.data.items[0].tempLocked"));

                        // matched by email too
                        mockMvc.perform(get("/admin/users").cookie(admin).param("q", marker + "@example"))
                                        .andExpect(jsonPath("$.data.totalElements").value(1));
                        // no match -> empty page
                        mockMvc.perform(get("/admin/users").cookie(admin).param("q", "nope" + uniq()))
                                        .andExpect(jsonPath("$.data.totalElements").value(0));
                }

                @Test
                void list_shouldSortByUsernameBothDirections() throws Exception {
                        Cookie admin = adminToken();
                        String tag = "ord" + uniq();
                        createStaff(admin, tag + "a@example.com", tag + "aaa", uniquePhone());
                        createStaff(admin, tag + "b@example.com", tag + "bbb", uniquePhone());

                        MvcResult asc = mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", tag).param("sort", "username").param("dir", "asc"))
                                        .andExpect(status().isOk()).andReturn();
                        List<String> ascNames = JsonPath.read(asc.getResponse().getContentAsString(),
                                        "$.data.items[*].username");
                        assertEquals(List.of(tag + "aaa", tag + "bbb"), ascNames);

                        MvcResult desc = mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", tag).param("sort", "username").param("dir", "desc"))
                                        .andExpect(status().isOk()).andReturn();
                        List<String> descNames = JsonPath.read(desc.getResponse().getContentAsString(),
                                        "$.data.items[*].username");
                        assertEquals(List.of(tag + "bbb", tag + "aaa"), descNames);
                }

                @Test
                void list_shouldFilterByRoleIdAndStatus() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "flt" + uniq();
                        createStaff(admin, marker + "@example.com", marker, uniquePhone());

                        mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", marker).param("roleId", String.valueOf(roleId("Reception"))))
                                        .andExpect(jsonPath("$.data.totalElements").value(1));
                        mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", marker).param("roleId", String.valueOf(roleId("Records"))))
                                        .andExpect(jsonPath("$.data.totalElements").value(0));
                        mockMvc.perform(get("/admin/users").cookie(admin)
                                        .param("q", marker).param("status", "INACTIVE"))
                                        .andExpect(jsonPath("$.data.totalElements").value(0));
                }

                @Test
                void list_shouldClampSizeAndRejectBadSortDirAndStatus() throws Exception {
                        Cookie admin = adminToken();

                        mockMvc.perform(get("/admin/users").cookie(admin).param("size", "1000"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.size").value(100));
                        // sort injection guard
                        mockMvc.perform(get("/admin/users").cookie(admin).param("sort", "password"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10010));
                        mockMvc.perform(get("/admin/users").cookie(admin).param("dir", "sideways"))
                                        .andExpect(status().isBadRequest());
                        // bad enum literal -> 400 via MethodArgumentTypeMismatch handler
                        mockMvc.perform(get("/admin/users").cookie(admin).param("status", "NOPE"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void list_shouldRequireUserViewRight() throws Exception {
                        Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
                        mockMvc.perform(get("/admin/users").cookie(citizen))
                                        .andExpect(status().isForbidden());
                        mockMvc.perform(get("/admin/users"))
                                        .andExpect(status().isUnauthorized());
                }
        }

        @Nested
        @DisplayName("AC-Create - staff account creation")
        class CreateStaff {

                @Test
                void create_shouldPersistStaffFieldsAndDefaultAssignedArea() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "crt" + uniq();

                        MvcResult created = mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "@example.com", marker, uniquePhone(),
                                                        roleId("Appraisal"), null)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                                        .andExpect(jsonPath("$.data.position").value("Can bo IT"))
                                        .andExpect(jsonPath("$.data.roleName").value("Appraisal"))
                                        .andReturn();
                        String body = created.getResponse().getContentAsString();
                        // blank assignedArea falls back to app.default-assigned-area
                        assertEquals("Xã Minh Tân", JsonPath.read(body, "$.data.assignedArea"));
                        // never leaks the password, even hashed
                        assertFalse(body.contains("password"));

                        String id = JsonPath.read(body, "$.data.id");
                        assertTrue(systemLogRepository.findAll().stream()
                                        .anyMatch(log -> "CREATE_USER".equals(log.getAction())
                                                        && id.equals(log.getEntityId())));
                }

                @Test
                void create_withExplicitAssignedArea_shouldKeepIt() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "area" + uniq();
                        mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "@example.com", marker, uniquePhone(),
                                                        roleId("Reception"), "Xa Khac")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.assignedArea").value("Xa Khac"));
                }

                @Test
                void create_withAllThreeDuplicates_shouldGatherAllErrors() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "dup" + uniq();
                        String email = marker + "@example.com";
                        String phone = uniquePhone();
                        createStaff(admin, email, marker, phone);

                        MvcResult res = mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(email, marker, phone, roleId("Reception"), null)))
                                        .andExpect(status().isBadRequest())
                                        .andReturn();
                        JSONArray codes = JsonPath.read(res.getResponse().getContentAsString(), "$[*].code");
                        assertTrue(codes.containsAll(List.of(1002, 1003, 10023)));
                }

                @Test
                void create_withCitizenRole_shouldReturnCitizenRoleRestricted() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "ctz" + uniq();
                        mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "@example.com", marker, uniquePhone(),
                                                        roleId("Citizen"), null)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10024));
                }

                @Test
                void create_withInvalidPhoneOrShortUsername_shouldReturn400() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "val" + uniq();
                        // bad phone
                        mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "@example.com", marker, "12345",
                                                        roleId("Reception"), null)))
                                        .andExpect(status().isBadRequest());
                        // username shorter than 4
                        mockMvc.perform(post("/admin/users").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "b@example.com", "ab", uniquePhone(),
                                                        roleId("Reception"), null)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void create_shouldRequireUserCreateRight() throws Exception {
                        // Head holds USER_VIEW but not USER_CREATE
                        Cookie head = cookieOf(registerUserWithRole("Head"), "access_token");
                        String marker = "perm" + uniq();
                        mockMvc.perform(post("/admin/users").cookie(head)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(staffBody(marker + "@example.com", marker, uniquePhone(),
                                                        roleId("Reception"), null)))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("AC-Update - BUG-2 regression and citizen guards")
        class UpdateStaff {

                @Test
                void update_withAnotherUsersEmail_shouldReturn400NotStack500() throws Exception {
                        Cookie admin = adminToken();
                        String m1 = "upda" + uniq();
                        String m2 = "updb" + uniq();
                        createStaff(admin, m1 + "@example.com", m1, uniquePhone());
                        String otherId = createStaff(admin, m2 + "@example.com", m2, uniquePhone());

                        MvcResult res = mockMvc.perform(put("/admin/users/" + otherId).cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"email\": \"" + m1 + "@example.com\" }"))
                                        .andExpect(status().isBadRequest())
                                        .andReturn();
                        JSONArray codes = JsonPath.read(res.getResponse().getContentAsString(), "$[*].code");
                        assertTrue(codes.contains(1002));
                }

                @Test
                void update_keepingOwnEmail_shouldSucceed() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "self" + uniq();
                        String id = createStaff(admin, marker + "@example.com", marker, uniquePhone());

                        mockMvc.perform(put("/admin/users/" + id).cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"email\": \"" + marker + "@example.com\", \"name\": \"Renamed\" }"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.name").value("Renamed"));
                }

                @Test
                void update_assigningCitizenRole_shouldReturnCitizenRoleRestricted() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "toctz" + uniq();
                        String id = createStaff(admin, marker + "@example.com", marker, uniquePhone());

                        mockMvc.perform(put("/admin/users/" + id).cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"roleId\": " + roleId("Citizen") + " }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10024));
                }

                @Test
                void update_changingRoleOfCitizenAccount_shouldReturnCitizenRoleRestricted() throws Exception {
                        Cookie admin = adminToken();
                        MvcResult citizen = registerUser(uniqueEmail(), uniquePhone());
                        String citizenId = userIdFromBody(citizen);

                        mockMvc.perform(put("/admin/users/" + citizenId).cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"roleId\": " + roleId("Reception") + " }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10024));
                }
        }

        @Nested
        @DisplayName("AC-Status - deactivate / reactivate lifecycle")
        class StatusToggle {

                @Test
                void deactivate_blocksLoginAndRefresh_thenReactivateRestoresAccess() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "life" + uniq();
                        String email = marker + "@example.com";
                        String id = createStaff(admin, email, marker, uniquePhone());

                        // login before deactivation to obtain a live refresh cookie
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(200, login.getResponse().getStatus());
                        Cookie refresh = cookieOf(login, "refresh_token");
                        String familyId = refreshTokenRepository.findById(refreshTokenId(refresh))
                                        .orElseThrow().getFamilyId();

                        // deactivate
                        mockMvc.perform(put("/admin/users/" + id + "/status").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"INACTIVE\" }"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.status").value("INACTIVE"));
                        assertTrue(systemLogRepository.findAll().stream()
                                        .anyMatch(log -> "USER_DEACTIVATE".equals(log.getAction())
                                                        && id.equals(log.getEntityId())
                                                        && "ACTIVE".equals(log.getOldValue())
                                                        && "INACTIVE".equals(log.getNewValue())));

                        // login is blocked with ACCOUNT_INACTIVE (10027)
                        MvcResult blocked = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(403, blocked.getResponse().getStatus());
                        assertEquals(Integer.valueOf(10027),
                                        JsonPath.read(blocked.getResponse().getContentAsString(), "$.code"));

                        // refresh with the pre-deactivation cookie is rejected + family revoked
                        mockMvc.perform(post("/auth/refresh").cookie(refresh))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.code").value(10027));
                        assertTrue(refreshTokenRepository.findByFamilyId(familyId).stream()
                                        .allMatch(RefreshToken::isRevoked));

                        // reactivate -> login works again
                        mockMvc.perform(put("/admin/users/" + id + "/status").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"ACTIVE\" }"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
                        assertTrue(systemLogRepository.findAll().stream()
                                        .anyMatch(log -> "USER_ACTIVATE".equals(log.getAction())
                                                        && id.equals(log.getEntityId())));
                        assertEquals(200, loginAs(email, DEFAULT_PASSWORD, uniqueIp())
                                        .getResponse().getStatus());
                }

                @Test
                void bannedUser_refresh_shouldBeRejected() throws Exception {
                        // GAP-6 regression: refresh used to ignore status entirely
                        String email = uniqueEmail();
                        MvcResult reg = registerUser(email, uniquePhone());
                        String userId = userIdFromBody(reg);
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        Cookie refresh = cookieOf(login, "refresh_token");

                        User user = userRepository.getUserById(userId);
                        user.setStatus(AccountStatus.BANNED);
                        userRepository.save(user);

                        mockMvc.perform(post("/auth/refresh").cookie(refresh))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.code").value(10022));
                }

                @Test
                void selfDeactivation_shouldReturnCannotDeactivateSelf() throws Exception {
                        MvcResult admin = registerUserWithRole("Admin");
                        Cookie token = cookieOf(admin, "access_token");
                        String ownId = userIdFromBody(admin);

                        mockMvc.perform(put("/admin/users/" + ownId + "/status").cookie(token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"INACTIVE\" }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10025));
                }

                @Test
                void unchangedOrIllegalStatus_shouldReturnInvalidStatus() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "inv" + uniq();
                        String id = createStaff(admin, marker + "@example.com", marker, uniquePhone());

                        // already ACTIVE -> setting ACTIVE again is a 10026
                        mockMvc.perform(put("/admin/users/" + id + "/status").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"ACTIVE\" }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10026));
                        // BANNED has no admin flow yet
                        mockMvc.perform(put("/admin/users/" + id + "/status").cookie(admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"BANNED\" }"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10026));
                }

                @Test
                void statusToggle_shouldRequireUserDeactivateRight() throws Exception {
                        Cookie admin = adminToken();
                        String marker = "stperm" + uniq();
                        String id = createStaff(admin, marker + "@example.com", marker, uniquePhone());
                        // Head holds USER_VIEW but not USER_DEACTIVATE
                        Cookie head = cookieOf(registerUserWithRole("Head"), "access_token");

                        mockMvc.perform(put("/admin/users/" + id + "/status").cookie(head)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{ \"status\": \"INACTIVE\" }"))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("AC-Roles - view-only roles screen (GAP-7, §4.8)")
        class RolesScreen {

                @Test
                void userViewOnlyActor_shouldReadRolesWithCodeAndUserCount() throws Exception {
                        // GAP-7: Head has USER_VIEW but neither PERMISSION_MANAGE nor ROLE_MANAGE
                        Cookie head = cookieOf(registerUserWithRole("Head"), "access_token");

                        MvcResult res = mockMvc.perform(get("/admin/roles").cookie(head))
                                        .andExpect(status().isOk())
                                        .andReturn();
                        String body = res.getResponse().getContentAsString();

                        List<Integer> ids = JsonPath.read(body, "$.data[*].id");
                        assertEquals(ids.stream().sorted().toList(), ids, "roles sorted by id asc");

                        // every seeded role carries its stable code
                        RoleCodes.NAME_TO_CODE.forEach((name, code) -> {
                                JSONArray match = JsonPath.read(body,
                                                "$.data[?(@.name == '" + name + "')].code");
                                assertEquals(code, match.get(0), "code of " + name);
                        });

                        // this test itself registered a citizen account -> Citizen userCount >= 1
                        JSONArray citizenCount = JsonPath.read(body,
                                        "$.data[?(@.code == 'CITIZEN')].userCount");
                        assertTrue(((Number) citizenCount.get(0)).longValue() >= 1);
                }

                @Test
                void creatingStaff_shouldIncrementThatRolesUserCount() throws Exception {
                        Cookie admin = adminToken();

                        long before = receptionUserCount(admin);
                        String marker = "cnt" + uniq();
                        createStaff(admin, marker + "@example.com", marker, uniquePhone());
                        long after = receptionUserCount(admin);

                        assertEquals(before + 1, after);
                }

                private long receptionUserCount(Cookie token) throws Exception {
                        MvcResult res = mockMvc.perform(get("/admin/roles").cookie(token))
                                        .andExpect(status().isOk()).andReturn();
                        JSONArray count = JsonPath.read(res.getResponse().getContentAsString(),
                                        "$.data[?(@.code == 'RECEPTION_OFFICER')].userCount");
                        return ((Number) count.get(0)).longValue();
                }

                @Test
                void citizenActor_shouldStillBeForbidden() throws Exception {
                        Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
                        mockMvc.perform(get("/admin/roles").cookie(citizen))
                                        .andExpect(status().isForbidden());
                }
        }
}
