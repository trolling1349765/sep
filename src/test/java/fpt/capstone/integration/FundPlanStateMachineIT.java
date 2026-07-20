package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fund-usage plan state machine (UC 5.7.6): create → approve (reserve pending) →
 * complete (spend) / cancel (rollback); reject → resubmit; soft-delete. Verifies the
 * money invariant {@code available = amount - pending - spent} after each transition.
 * Creator = Management (FUNDING_ALLOCATE), approver = Appraisal (FUNDING_PLAN_APPROVE).
 */
class FundPlanStateMachineIT extends AbstractIntegrationTest {

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie appraisal() throws Exception {
        return cookieOf(registerUserWithRole("Appraisal"), "access_token");
    }

    private MockMultipartFile file(String field) {
        return new MockMultipartFile(field, "f.pdf", "application/pdf", "x".getBytes());
    }

    /** Records a CONFIRMED funding (with evidence) and returns its id. */
    private int confirmedFunding(Cookie mgmt, String amount) throws Exception {
        String json = """
                {"name":"Quy %s","amount":%s,"receivedDate":"2026-01-01","purpose":"Ho tro",
                 "paymentMethod":"CASH","evidenceName":"BB"}
                """.formatted(uniq(), amount);
        MvcResult res = mockMvc.perform(multipart("/fundings")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private String planJson(int donationId, String amount) {
        return """
                {"donationId":%d,"programName":"CT %s","amount":%s,"purpose":"Mua xe lan","expectedDate":"2026-12-31"}
                """.formatted(donationId, uniq(), amount);
    }

    private String createPlan(Cookie mgmt, int donationId, String amount) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/fund-plans")
                        .file(new MockMultipartFile("data", "", "application/json", planJson(donationId, amount).getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private void assertFundingBalance(Cookie token, int fundingId, long available, long pending, long spent)
            throws Exception {
        mockMvc.perform(get("/fundings/{id}", fundingId).cookie(token))
                .andExpect(jsonPath("$.data.availableAmount").value(available))
                .andExpect(jsonPath("$.data.pendingAmount").value(pending))
                .andExpect(jsonPath("$.data.spentAmount").value(spent));
    }

    @Nested
    @DisplayName("happy path: create → approve → complete")
    class HappyPath {

        @Test
        void fullFlow_shouldReserveThenSpend_andAudit() throws Exception {
            Cookie mgmt = management();
            Cookie appr = appraisal();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "60000000");

            // create must not touch balance
            assertFundingBalance(mgmt, funding, 100000000L, 0, 0);

            mockMvc.perform(post("/fund-plans/{id}/approve", plan).cookie(appr))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
            assertFundingBalance(mgmt, funding, 40000000L, 60000000L, 0);

            mockMvc.perform(multipart("/fund-plans/{id}/complete", plan)
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
            assertFundingBalance(mgmt, funding, 40000000L, 0, 60000000L);

            assertTrue(systemLogRepository.findAll().stream()
                    .anyMatch(log -> "FUND_PLAN_APPROVE".equals(log.getAction()) && plan.equals(log.getEntityId())));
        }

        @Test
        void approveThenCancel_shouldRollbackPending() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "60000000");
            mockMvc.perform(post("/fund-plans/{id}/approve", plan).cookie(appraisal()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/fund-plans/{id}/cancel", plan).cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"khong can nua\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
            assertFundingBalance(mgmt, funding, 100000000L, 0, 0);
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        void create_overAvailable_shouldReturn10041() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            mockMvc.perform(multipart("/fund-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(funding, "120000000").getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10041));
        }

        @Test
        void approve_secondPlanOverAvailable_shouldReturn10041() throws Exception {
            Cookie mgmt = management();
            Cookie appr = appraisal();
            int funding = confirmedFunding(mgmt, "100000000");
            // both created while 100M available
            String a = createPlan(mgmt, funding, "60000000");
            String b = createPlan(mgmt, funding, "60000000");
            mockMvc.perform(post("/fund-plans/{id}/approve", a).cookie(appr)).andExpect(status().isOk());
            mockMvc.perform(post("/fund-plans/{id}/approve", b).cookie(appr))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10041));
        }

        @Test
        void create_missingList_shouldReturn10038() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            mockMvc.perform(multipart("/fund-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(funding, "1000000").getBytes()))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10038));
        }

        @Test
        void deleteApproved_shouldReturn10043() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "10000000");
            mockMvc.perform(post("/fund-plans/{id}/approve", plan).cookie(appraisal())).andExpect(status().isOk());
            mockMvc.perform(post("/fund-plans/{id}/delete", plan).cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"x\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10043));
        }
    }

    @Nested
    @DisplayName("reject/resubmit + soft delete")
    class RejectAndDelete {

        @Test
        void reject_thenUpdate_shouldReturnToPending() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "10000000");
            mockMvc.perform(post("/fund-plans/{id}/reject", plan).cookie(appraisal())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"thieu ho so\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));

            mockMvc.perform(multipart("/fund-plans/{id}", plan)
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(funding, "12000000").getBytes()))
                            .with(req -> {
                                req.setMethod("PUT");
                                return req;
                            })
                            .cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
        }

        @Test
        void reject_missingReason_shouldReturn10045() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "10000000");
            mockMvc.perform(post("/fund-plans/{id}/reject", plan).cookie(appraisal())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"  \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10045));
        }

        @Test
        void softDelete_shouldHideFromDefaultListButRemainVisibleByStatus() throws Exception {
            Cookie mgmt = management();
            int funding = confirmedFunding(mgmt, "100000000");
            String plan = createPlan(mgmt, funding, "10000000");
            mockMvc.perform(post("/fund-plans/{id}/delete", plan).cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"nhap sai\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DELETED"));

            // default list (filtered by this donation) hides DELETED
            mockMvc.perform(get("/fund-plans").cookie(mgmt).param("donationId", String.valueOf(funding)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
            // explicit status=DELETED reveals it
            mockMvc.perform(get("/fund-plans").cookie(mgmt)
                            .param("donationId", String.valueOf(funding)).param("status", "DELETED"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
            // detail still returns it with the reason
            mockMvc.perform(get("/fund-plans/{id}", plan).cookie(mgmt))
                    .andExpect(jsonPath("$.data.deleteReason").value("nhap sai"));
        }
    }
}
