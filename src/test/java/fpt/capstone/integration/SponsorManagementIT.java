package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sponsor management (UC 5.7.1–5.7.3): create with BR-47 dedupe, update keeping
 * history, ACTIVE<->INACTIVE toggle, RBAC, no-delete. Inherits the exact
 * bean-override set of AbstractIntegrationTest — do not add @MockitoBean here.
 */
class SponsorManagementIT extends AbstractIntegrationTest {

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie citizen() throws Exception {
        return cookieOf(registerUserWithRole("Citizen"), "access_token");
    }

    private String body(String name, String orgCode, String phone) {
        return """
                {
                  "name": "%s",
                  "type": "ORG",
                  "orgCode": "%s",
                  "contactPerson": "Nguyen Van A",
                  "phone": "%s",
                  "email": "%s",
                  "address": "Ha Noi",
                  "note": "n"
                }
                """.formatted(name, orgCode, phone, uniqueEmail());
    }

    private String createSponsor(Cookie token, String name, String orgCode, String phone) throws Exception {
        MvcResult res = mockMvc.perform(post("/sponsors").cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, orgCode, phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.startsWith("NTT-")))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Nested
    @DisplayName("create + dedupe (BR-47)")
    class CreateDedupe {

        @Test
        void create_shouldPersistAndWriteAudit() throws Exception {
            Cookie token = management();
            String id = createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), uniquePhone());

            assertTrue(systemLogRepository.findAll().stream()
                    .anyMatch(log -> "SPONSOR_CREATE".equals(log.getAction())
                            && id.equals(log.getEntityId())));
        }

        @Test
        void create_duplicatePhone_shouldReturn10035WithDuplicateData() throws Exception {
            Cookie token = management();
            String phone = uniquePhone();
            createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), phone);

            mockMvc.perform(post("/sponsors").cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Quy " + uniq(), "ORG-" + uniq(), phone)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10035))
                    .andExpect(jsonPath("$.data.duplicateId").exists());
        }

        @Test
        void create_duplicateNormalizedName_shouldReturn10035() throws Exception {
            Cookie token = management();
            String base = "Quy Tu Thien " + uniq();
            createSponsor(token, base, "ORG-" + uniq(), uniquePhone());

            // same name with accents + different casing/spacing normalizes equal
            mockMvc.perform(post("/sponsors").cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("  " + base.toUpperCase() + "  ", "ORG-" + uniq(), uniquePhone())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10035));
        }

        @Test
        void create_invalidPhone_shouldReturnValidationError() throws Exception {
            mockMvc.perform(post("/sponsors").cookie(management())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Quy " + uniq(), "ORG-" + uniq(), "abc123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10010));
        }
    }

    @Nested
    @DisplayName("read + update")
    class ReadUpdate {

        @Test
        void getById_shouldReturnDetailWithHistoryArray() throws Exception {
            Cookie token = management();
            String id = createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), uniquePhone());

            mockMvc.perform(get("/sponsors/{id}", id).cookie(token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.contributionHistory").isArray());
        }

        @Test
        void list_shouldFindByQuery() throws Exception {
            Cookie token = management();
            String marker = "Marker" + uniq();
            createSponsor(token, marker, "ORG-" + uniq(), uniquePhone());

            mockMvc.perform(get("/sponsors").cookie(token)
                            .param("q", marker))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].name").value(marker))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void update_shouldSucceedAndBlockDuplicate() throws Exception {
            Cookie token = management();
            String phoneA = uniquePhone();
            String idA = createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), phoneA);
            String phoneB = uniquePhone();
            createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), phoneB);

            // valid self-update keeps its own phone
            mockMvc.perform(put("/sponsors/{id}", idA).cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Quy Renamed " + uniq(), "ORG-" + uniq(), phoneA)))
                    .andExpect(status().isOk());

            // moving A onto B's phone is a duplicate
            mockMvc.perform(put("/sponsors/{id}", idA).cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Quy " + uniq(), "ORG-" + uniq(), phoneB)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10035));
        }
    }

    @Nested
    @DisplayName("status toggle")
    class StatusToggle {

        @Test
        void changeStatus_toggleThenSameAgain_shouldReturn10026() throws Exception {
            Cookie token = management();
            String id = createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), uniquePhone());

            mockMvc.perform(put("/sponsors/{id}/status", id).cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("INACTIVE"));

            mockMvc.perform(put("/sponsors/{id}/status", id).cookie(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10026));
        }
    }

    @Nested
    @DisplayName("RBAC + no delete")
    class RbacAndNoDelete {

        @Test
        void citizen_cannotCreateOrView() throws Exception {
            Cookie citizen = citizen();
            mockMvc.perform(post("/sponsors").cookie(citizen)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Quy " + uniq(), "ORG-" + uniq(), uniquePhone())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/sponsors").cookie(citizen))
                    .andExpect(status().isForbidden());
        }

        @Test
        void delete_shouldNotBeRouted() throws Exception {
            Cookie token = management();
            String id = createSponsor(token, "Quy " + uniq(), "ORG-" + uniq(), uniquePhone());
            mockMvc.perform(delete("/sponsors/{id}", id).cookie(token))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
