package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Funding recording (UC 5.7.4–5.7.6): evidence-driven DRAFT/CONFIRMED, TRANSFER
 * transaction-ref rule, DRAFT-only edit lock, confirm gate, RBAC. Establishes the
 * module's multipart convention (JSON part "data" + repeated "files").
 */
class FundingManagementIT extends AbstractIntegrationTest {

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie citizen() throws Exception {
        return cookieOf(registerUserWithRole("Citizen"), "access_token");
    }

    private String body(String name, String amount, String method, String ref) {
        String refLine = ref == null ? "" : "\"transactionRef\": \"" + ref + "\",";
        return """
                {
                  "name": "%s",
                  "amount": %s,
                  "receivedDate": "2026-01-01",
                  "purpose": "Ho tro",
                  "paymentMethod": "%s",
                  %s
                  "evidenceName": "Bien ban"
                }
                """.formatted(name, amount, method, refLine);
    }

    private MockMultipartFile dataPart(String json) {
        return new MockMultipartFile("data", "", "application/json", json.getBytes());
    }

    private MockMultipartFile filePart() {
        return new MockMultipartFile("files", "e.pdf", "application/pdf", "evidence".getBytes());
    }

    /** POST /fundings with data + optional file; returns the created id. */
    private int createFunding(Cookie token, String json, boolean withFile) throws Exception {
        MockMultipartHttpServletRequestBuilder rb = multipart("/fundings").file(dataPart(json));
        if (withFile) {
            rb.file(filePart());
        }
        MvcResult res = mockMvc.perform(rb.cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.startsWith("KP-")))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void create_withEvidence_shouldBeConfirmedAndAudited() throws Exception {
            Cookie token = management();
            int id = createFunding(token, body("Quy " + uniq(), "100000000", "CASH", null), true);

            mockMvc.perform(get("/fundings/{id}", id).cookie(token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.availableAmount").value(100000000))
                    .andExpect(jsonPath("$.data.attachments.length()").value(1));

            assertTrue(systemLogRepository.findAll().stream()
                    .anyMatch(log -> "FUNDING_CREATE".equals(log.getAction())
                            && String.valueOf(id).equals(log.getEntityId())));
        }

        @Test
        void create_withoutEvidence_shouldBeDraft() throws Exception {
            Cookie token = management();
            int id = createFunding(token, body("Quy " + uniq(), "5000000", "CASH", null), false);
            mockMvc.perform(get("/fundings/{id}", id).cookie(token))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        void create_transferWithoutRef_shouldReturn10039() throws Exception {
            mockMvc.perform(multipart("/fundings")
                            .file(dataPart(body("Quy " + uniq(), "100000", "TRANSFER", null)))
                            .file(filePart())
                            .cookie(management()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10039));
        }

        @Test
        void create_nonPositiveAmount_shouldReturnValidationError() throws Exception {
            mockMvc.perform(multipart("/fundings")
                            .file(dataPart(body("Quy " + uniq(), "0", "CASH", null)))
                            .cookie(management()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10010));
        }
    }

    @Nested
    @DisplayName("edit lock + confirm")
    class EditAndConfirm {

        @Test
        void put_afterConfirmed_shouldReturnFundingLocked() throws Exception {
            Cookie token = management();
            int id = createFunding(token, body("Quy " + uniq(), "100000000", "CASH", null), true);

            mockMvc.perform(multipart(HttpMethod.PUT, "/fundings/{id}", id)
                            .file(dataPart(body("Quy Renamed", "200000000", "CASH", null)))
                            .cookie(token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10040));
        }

        @Test
        void confirm_flow_missingThenSuccessThenLocked() throws Exception {
            Cookie token = management();
            int id = createFunding(token, body("Quy " + uniq(), "100000000", "CASH", null), false);

            // DRAFT without evidence cannot be confirmed
            mockMvc.perform(put("/fundings/{id}/confirm", id).cookie(token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10038));

            // append evidence via PUT (stays DRAFT), then confirm succeeds
            mockMvc.perform(multipart(HttpMethod.PUT, "/fundings/{id}", id)
                            .file(dataPart(body("Quy " + uniq(), "100000000", "CASH", null)))
                            .file(filePart())
                            .cookie(token))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/fundings/{id}/confirm", id).cookie(token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

            // second confirm is locked
            mockMvc.perform(put("/fundings/{id}/confirm", id).cookie(token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10040));
        }
    }

    @Nested
    @DisplayName("read + rbac")
    class ReadRbac {

        @Test
        void getById_notFound_shouldReturn10036() throws Exception {
            mockMvc.perform(get("/fundings/{id}", 999999).cookie(management()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(10036));
        }

        @Test
        void citizen_cannotCreate() throws Exception {
            mockMvc.perform(multipart("/fundings")
                            .file(dataPart(body("Quy " + uniq(), "100000", "CASH", null)))
                            .cookie(citizen()))
                    .andExpect(status().isForbidden());
        }
    }
}
