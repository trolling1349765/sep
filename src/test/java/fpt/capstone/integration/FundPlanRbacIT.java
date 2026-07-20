package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Two-layer role separation for fund plans (Đợt 6 §4.2 preview): the creator role
 * (Management, FUNDING_ALLOCATE) is blocked from approve/reject at the RBAC layer,
 * and the approver role (Appraisal, FUNDING_PLAN_APPROVE) is blocked from create.
 */
class FundPlanRbacIT extends AbstractIntegrationTest {

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie appraisal() throws Exception {
        return cookieOf(registerUserWithRole("Appraisal"), "access_token");
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("files", "f.pdf", "application/pdf", "x".getBytes());
    }

    private int confirmedFunding(Cookie mgmt) throws Exception {
        String json = """
                {"name":"Quy %s","amount":100000000,"receivedDate":"2026-01-01","purpose":"Ho tro",
                 "paymentMethod":"CASH","evidenceName":"BB"}
                """.formatted(uniq());
        MvcResult res = mockMvc.perform(multipart("/fundings")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file())
                        .cookie(mgmt))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private String createPlan(Cookie mgmt, int funding) throws Exception {
        String json = """
                {"donationId":%d,"programName":"CT %s","amount":10000000,"purpose":"p","expectedDate":"2026-12-31"}
                """.formatted(funding, uniq());
        MvcResult res = mockMvc.perform(multipart("/fund-plans")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file())
                        .cookie(mgmt))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Test
    void management_cannotApproveOrReject() throws Exception {
        Cookie mgmt = management();
        int funding = confirmedFunding(mgmt);
        String plan = createPlan(mgmt, funding);

        mockMvc.perform(post("/fund-plans/{id}/approve", plan).cookie(mgmt))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/fund-plans/{id}/reject", plan).cookie(mgmt)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void appraisal_cannotCreatePlan() throws Exception {
        Cookie mgmt = management();
        int funding = confirmedFunding(mgmt);
        String json = """
                {"donationId":%d,"programName":"CT %s","amount":10000000,"purpose":"p","expectedDate":"2026-12-31"}
                """.formatted(funding, uniq());
        mockMvc.perform(multipart("/fund-plans")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file())
                        .cookie(appraisal()))
                .andExpect(status().isForbidden());
    }
}
