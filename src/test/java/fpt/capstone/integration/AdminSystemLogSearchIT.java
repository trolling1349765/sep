package fpt.capstone.integration;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /admin/logs free-text q + severity filter, and GET /admin/logs/{id}
 * (both AUDIT_LOG_VIEW). Log rows are seeded straight through the repository
 * with uniq() markers so filters can be asserted deterministically against
 * the shared, ever-growing system_log table.
 */
class AdminSystemLogSearchIT extends AbstractIntegrationTest {

    private Cookie adminToken() throws Exception {
        return cookieOf(registerUserWithRole("Admin"), "access_token");
    }

    private SystemLog seedLog(String action, String userId, String entityId) {
        return systemLogRepository.save(SystemLog.builder()
                .userId(userId)
                .action(action)
                .entityType("USERS")
                .entityId(entityId)
                .oldValue("old-" + entityId)
                .newValue("new-" + entityId)
                .ipAddress("10.0.0.1")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Nested
    @DisplayName("GET /admin/logs?q=...")
    class FreeTextSearch {

        @Test
        void q_matchesActionCaseInsensitively() throws Exception {
            Cookie admin = adminToken();
            String marker = "e-" + uniq();
            seedLog("USER_LOGIN", null, marker);

            mockMvc.perform(get("/admin/logs").cookie(admin)
                            .param("q", "user_login")
                            .param("entityType", "USERS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].action").value("USER_LOGIN"));
        }

        @Test
        void q_matchesEntityIdAndContent() throws Exception {
            Cookie admin = adminToken();
            String marker = "e-" + uniq();
            seedLog("USER_LOGIN", null, marker);

            mockMvc.perform(get("/admin/logs").cookie(admin).param("q", marker.toUpperCase()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].entityId").value(marker));

            // oldValue/newValue are searched too
            mockMvc.perform(get("/admin/logs").cookie(admin).param("q", "new-" + marker))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void q_matchesActorName() throws Exception {
            Cookie admin = adminToken();
            String actorName = "Nguyễn Văn Bình " + uniq();
            String userId = userIdFromBody(registerUser(uniqueEmail(), uniquePhone()));
            User actor = userRepository.getUserById(userId);
            actor.setName(actorName);
            userRepository.save(actor);
            String marker = "e-" + uniq();
            seedLog("USER_LOGIN", userId, marker);

            mockMvc.perform(get("/admin/logs").cookie(admin)
                            .param("q", actorName.toLowerCase())
                            .param("action", "USER_LOGIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].entityId").value(marker))
                    .andExpect(jsonPath("$.data.items[0].actorName").value(actorName));
        }

        @Test
        void q_noMatch_returnsEmptyPageWithZeroTotal() throws Exception {
            mockMvc.perform(get("/admin/logs").cookie(adminToken())
                            .param("q", "no-such-marker-" + uniq()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /admin/logs?severity=...")
    class SeverityFilter {

        @Test
        void severity_partitionsRowsByDerivedLevel() throws Exception {
            Cookie admin = adminToken();
            String marker = "sev-" + uniq();
            seedLog("RESTORE_START", null, marker);   // CRITICAL
            seedLog("CHANGE_ROLE", null, marker);     // WARNING
            seedLog("USER_LOGIN", null, marker);      // INFO

            mockMvc.perform(get("/admin/logs").cookie(admin)
                            .param("q", marker).param("severity", "CRITICAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].action").value("RESTORE_START"))
                    .andExpect(jsonPath("$.data.items[0].severity").value("CRITICAL"));

            mockMvc.perform(get("/admin/logs").cookie(admin)
                            .param("q", marker).param("severity", "WARNING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].action").value("CHANGE_ROLE"));

            mockMvc.perform(get("/admin/logs").cookie(admin)
                            .param("q", marker).param("severity", "INFO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].action").value("USER_LOGIN"))
                    .andExpect(jsonPath("$.data.items[0].severity").value("INFO"));
        }

        @Test
        void severity_invalidValue_returns400() throws Exception {
            mockMvc.perform(get("/admin/logs").cookie(adminToken()).param("severity", "FATAL"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10010));
        }

        @Test
        void everyListedItem_carriesSeverityField() throws Exception {
            Cookie admin = adminToken();
            seedLog("USER_LOGIN", null, "sev-any-" + uniq());

            mockMvc.perform(get("/admin/logs").cookie(admin).param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].severity").exists());
        }
    }

    @Nested
    @DisplayName("GET /admin/logs/{id}")
    class Detail {

        @Test
        void detail_returnsAllFieldsIncludingActorRoleAndIp() throws Exception {
            Cookie admin = adminToken();
            String userId = userIdFromBody(registerUserWithRole("Admin"));
            SystemLog saved = seedLog("PERMISSION_UPDATE", userId, "detail-" + uniq());

            mockMvc.perform(get("/admin/logs/" + saved.getId()).cookie(admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(saved.getId()))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.actorName").value("IT User"))
                    .andExpect(jsonPath("$.data.actorRole").value("Admin"))
                    .andExpect(jsonPath("$.data.action").value("PERMISSION_UPDATE"))
                    .andExpect(jsonPath("$.data.severity").value("WARNING"))
                    .andExpect(jsonPath("$.data.entityType").value("USERS"))
                    .andExpect(jsonPath("$.data.entityId").value(saved.getEntityId()))
                    .andExpect(jsonPath("$.data.oldValue").value(saved.getOldValue()))
                    .andExpect(jsonPath("$.data.newValue").value(saved.getNewValue()))
                    .andExpect(jsonPath("$.data.ipAddress").value("10.0.0.1"))
                    .andExpect(jsonPath("$.data.createdAt").exists());
        }

        @Test
        void detail_logWithoutActor_actorFieldsNull() throws Exception {
            Cookie admin = adminToken();
            SystemLog saved = seedLog("USER_LOGIN", null, "detail-" + uniq());

            mockMvc.perform(get("/admin/logs/" + saved.getId()).cookie(admin))
                    .andExpect(status().isOk())
                    // isEmpty() accepts both JSON null and an absent key
                    .andExpect(jsonPath("$.data.actorName").isEmpty())
                    .andExpect(jsonPath("$.data.actorRole").isEmpty());
        }

        @Test
        void detail_unknownId_returns404WithBusinessCode() throws Exception {
            mockMvc.perform(get("/admin/logs/99999999").cookie(adminToken()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(10028));
        }

        @Test
        void detail_asCitizen_returns403() throws Exception {
            Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
            mockMvc.perform(get("/admin/logs/1").cookie(citizen))
                    .andExpect(status().isForbidden());
        }

        @Test
        void detail_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/admin/logs/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Append-only")
    class AppendOnly {

        @Test
        void logs_haveNoUpdateOrDeleteEndpoints() throws Exception {
            Cookie admin = adminToken();
            SystemLog saved = seedLog("USER_LOGIN", null, "ro-" + uniq());

            mockMvc.perform(delete("/admin/logs/" + saved.getId()).cookie(admin))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(put("/admin/logs/" + saved.getId()).cookie(admin))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
