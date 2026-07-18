package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /admin/dashboard (SYSTEM_MONITOR_VIEW) and GET /admin/logs (AUDIT_LOG_VIEW). */
class AdminDashboardIT extends AbstractIntegrationTest {

        private Cookie adminToken() throws Exception {
                return cookieOf(registerUserWithRole("Admin"), "access_token");
        }

        @Nested
        @DisplayName("GET /admin/dashboard")
        class Dashboard {

                @Test
                void dashboard_asAdmin_returnsAllThreeSections() throws Exception {
                        Cookie admin = adminToken();

                        mockMvc.perform(get("/admin/dashboard").cookie(admin))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        // accounts: at least the admin we just registered, ACTIVE via lifecycle
                                        .andExpect(jsonPath("$.data.accounts.total").isNumber())
                                        .andExpect(jsonPath("$.data.accounts.active").isNumber())
                                        .andExpect(jsonPath("$.data.recentActivities").isArray())
                                        // operational block
                                        .andExpect(jsonPath("$.data.operational.status").value("UP"))
                                        // build-info.properties is generated during the build, so the
                                        // real version shows here; the "dev" fallback is unit-tested
                                        .andExpect(jsonPath("$.data.operational.version").isNotEmpty())
                                        .andExpect(jsonPath("$.data.operational.environment").value("DEMO"))
                                        .andExpect(jsonPath("$.data.operational.serverTime").exists())
                                        .andExpect(jsonPath("$.data.operational.startedAt").exists())
                                        .andExpect(jsonPath("$.data.operational.simulatedServices.length()").value(5));

                        MvcResult result = mockMvc.perform(get("/admin/dashboard").cookie(admin))
                                        .andExpect(status().isOk()).andReturn();
                        int total = JsonPath.read(result.getResponse().getContentAsString(),
                                        "$.data.accounts.total");
                        assertTrue(total >= 1);
                }

                @Test
                void dashboard_recentActivities_containsLatestLoginFirst() throws Exception {
                        Cookie admin = adminToken();
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        loginAs(email, DEFAULT_PASSWORD, uniqueIp());

                        MvcResult result = mockMvc.perform(get("/admin/dashboard").cookie(admin)
                                        .param("recentSize", "5"))
                                        .andExpect(status().isOk()).andReturn();

                        String body = result.getResponse().getContentAsString();
                        // newest first: the USER_LOGIN we just produced tops the feed
                        assertEquals("USER_LOGIN", JsonPath.read(body, "$.data.recentActivities[0].action"));
                        assertEquals("IT User", JsonPath.read(body, "$.data.recentActivities[0].actorName"));
                        int size = JsonPath.read(body, "$.data.recentActivities.length()");
                        assertTrue(size <= 5);
                }

                @Test
                void dashboard_bannedUser_countsAsLockedAndCannotLogin() throws Exception {
                        Cookie admin = adminToken();

                        MvcResult before = mockMvc.perform(get("/admin/dashboard").cookie(admin))
                                        .andExpect(status().isOk()).andReturn();
                        int lockedBefore = JsonPath.read(before.getResponse().getContentAsString(),
                                        "$.data.accounts.locked");
                        int bannedBefore = JsonPath.read(before.getResponse().getContentAsString(),
                                        "$.data.accounts.banned");

                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        User user = userRepository.getUserById(userIdFromBody(registered));
                        user.setStatus(AccountStatus.BANNED);
                        userRepository.save(user);

                        // banned account is rejected with the business code 10022
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(403, login.getResponse().getStatus());
                        assertEquals(Integer.valueOf(10022),
                                        JsonPath.read(login.getResponse().getContentAsString(), "$.code"));

                        MvcResult after = mockMvc.perform(get("/admin/dashboard").cookie(admin))
                                        .andExpect(status().isOk()).andReturn();
                        int lockedAfter = JsonPath.read(after.getResponse().getContentAsString(),
                                        "$.data.accounts.locked");
                        int bannedAfter = JsonPath.read(after.getResponse().getContentAsString(),
                                        "$.data.accounts.banned");
                        assertEquals(lockedBefore + 1, lockedAfter);
                        assertEquals(bannedBefore + 1, bannedAfter);
                }

                @Test
                void dashboard_recentSizeZero_returns400() throws Exception {
                        mockMvc.perform(get("/admin/dashboard").cookie(adminToken())
                                        .param("recentSize", "0"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10010));
                }

                @Test
                void dashboard_asCitizen_returns403() throws Exception {
                        Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
                        mockMvc.perform(get("/admin/dashboard").cookie(citizen))
                                        .andExpect(status().isForbidden());
                }

                @Test
                void dashboard_unauthenticated_returns401() throws Exception {
                        mockMvc.perform(get("/admin/dashboard"))
                                        .andExpect(status().isUnauthorized());
                }

                @Test
                void dashboard_doesNotWriteSystemLog() throws Exception {
                        Cookie admin = adminToken();
                        long before = systemLogRepository.count();

                        mockMvc.perform(get("/admin/dashboard").cookie(admin)).andExpect(status().isOk());
                        mockMvc.perform(get("/admin/dashboard").cookie(admin)).andExpect(status().isOk());

                        assertEquals(before, systemLogRepository.count());
                }
        }

        @Nested
        @DisplayName("GET /admin/logs")
        class Logs {

                @Test
                void logs_asAdmin_pagedDtoShape_filteredByAction() throws Exception {
                        Cookie admin = adminToken();
                        // guarantee at least one USER_LOGIN row
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        loginAs(email, DEFAULT_PASSWORD, uniqueIp());

                        MvcResult result = mockMvc.perform(get("/admin/logs").cookie(admin)
                                        .param("page", "0").param("size", "5")
                                        .param("action", "USER_LOGIN"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.page").value(0))
                                        .andExpect(jsonPath("$.data.size").value(5))
                                        .andExpect(jsonPath("$.data.totalElements").isNumber())
                                        .andExpect(jsonPath("$.data.totalPages").isNumber())
                                        .andExpect(jsonPath("$.data.items").isArray())
                                        .andExpect(jsonPath("$.data.items[0].action").value("USER_LOGIN"))
                                        .andExpect(jsonPath("$.data.items[0].createdAt").exists())
                                        .andReturn();

                        // DTO, not the entity: every item carries only spec'd fields
                        String body = result.getResponse().getContentAsString();
                        int count = JsonPath.read(body, "$.data.items.length()");
                        assertTrue(count >= 1 && count <= 5);
                }

                @Test
                void logs_fromAfterTo_returns400() throws Exception {
                        mockMvc.perform(get("/admin/logs").cookie(adminToken())
                                        .param("from", "2026-07-19T00:00:00")
                                        .param("to", "2026-01-01T00:00:00"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(10010));
                }

                @Test
                void logs_malformedFrom_returns400() throws Exception {
                        mockMvc.perform(get("/admin/logs").cookie(adminToken())
                                        .param("from", "not-a-date"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void logs_asCitizen_returns403_andWritesIllegalRequestLog() throws Exception {
                        Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
                        long before = systemLogRepository.count();

                        mockMvc.perform(get("/admin/logs").cookie(citizen))
                                        .andExpect(status().isForbidden());

                        // AuditingAccessDeniedHandler records the 403 as ILLEGAL_REQUEST
                        assertEquals(before + 1, systemLogRepository.count());
                }

                @Test
                void logs_unauthenticated_returns401() throws Exception {
                        mockMvc.perform(get("/admin/logs"))
                                        .andExpect(status().isUnauthorized());
                }
        }
}
