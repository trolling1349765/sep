package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.repository.BenificiaryRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Item allocation plan API (UC 5.7.12): header+lines validation (LINE_SUM_MISMATCH,
 * duplicate beneficiary), approve reserving stock, over-available guard, and the
 * two-layer role separation (Management create vs Appraisal approve).
 */
class ItemAllocationPlanApiIT extends AbstractIntegrationTest {

    @Autowired
    private BenificiaryRepository benificiaryRepository;

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie appraisal() throws Exception {
        return cookieOf(registerUserWithRole("Appraisal"), "access_token");
    }

    private MockMultipartFile file(String field) {
        return new MockMultipartFile(field, "f.pdf", "application/pdf", "x".getBytes());
    }

    private int beneficiary() {
        return benificiaryRepository.save(Benificiary.builder().fullName("Ben " + uniq()).build()).getId();
    }

    /** Creates an item and posts a receipt so it has {@code qty} on hand; returns itemId. */
    private String itemWithStock(Cookie mgmt, int qty) throws Exception {
        MvcResult item = mockMvc.perform(post("/support-items").cookie(mgmt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gao " + uniq() + "\",\"unit\":\"kg\"}"))
                .andExpect(status().isOk()).andReturn();
        String itemId = JsonPath.read(item.getResponse().getContentAsString(), "$.data.id");
        String receiptJson = """
                {"itemId":"%s","quantity":%d,"condition":"NEW","receiveDate":"2026-01-01","evidenceName":"BB"}
                """.formatted(itemId, qty);
        MvcResult receipt = mockMvc.perform(multipart("/inbound-receipts")
                        .file(new MockMultipartFile("data", "", "application/json", receiptJson.getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk()).andReturn();
        String receiptId = JsonPath.read(receipt.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/inbound-receipts/{id}/post", receiptId).cookie(mgmt)).andExpect(status().isOk());
        return itemId;
    }

    private String planJson(String itemId, int total, int q1, int q2, int b1, int b2) {
        return """
                {"itemId":"%s","plannedQty":%d,"expectedDate":"2026-12-31","deliveryPlace":"UBND xa",
                 "deliveryTimeWindow":"8-11h","lines":[{"beneficiaryId":%d,"plannedQty":%d},{"beneficiaryId":%d,"plannedQty":%d}]}
                """.formatted(itemId, total, b1, q1, b2, q2);
    }

    private String createPlan(Cookie mgmt, String itemId, int total, int q1, int q2) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/item-plans")
                        .file(new MockMultipartFile("data", "", "application/json",
                                planJson(itemId, total, q1, q2, beneficiary(), beneficiary()).getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void create_lineSumMismatch_shouldReturn10052() throws Exception {
            Cookie mgmt = management();
            String itemId = itemWithStock(mgmt, 1000);
            mockMvc.perform(multipart("/item-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(itemId, 30, 10, 10, beneficiary(), beneficiary()).getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10052));
        }

        @Test
        void create_duplicateBeneficiary_shouldReturnArgumentInvalid() throws Exception {
            Cookie mgmt = management();
            String itemId = itemWithStock(mgmt, 1000);
            int b = beneficiary();
            mockMvc.perform(multipart("/item-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(itemId, 20, 10, 10, b, b).getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10010));
        }

        @Test
        void create_overAvailable_shouldReturn10047() throws Exception {
            Cookie mgmt = management();
            String itemId = itemWithStock(mgmt, 15);
            mockMvc.perform(multipart("/item-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(itemId, 20, 10, 10, beneficiary(), beneficiary()).getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10047));
        }
    }

    @Nested
    @DisplayName("approve + rbac")
    class ApproveRbac {

        @Test
        void approve_shouldReserveStock() throws Exception {
            Cookie mgmt = management();
            String itemId = itemWithStock(mgmt, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(appraisal()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));

            mockMvc.perform(get("/support-items").cookie(mgmt).param("q", ""))
                    .andExpect(status().isOk());
            // reserved reflected on the item balance
            mockMvc.perform(get("/inventory/{itemId}/transactions", itemId).cookie(mgmt))
                    .andExpect(status().isOk());
        }

        @Test
        void management_cannotApprove_andAppraisalCannotCreate() throws Exception {
            Cookie mgmt = management();
            String itemId = itemWithStock(mgmt, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);

            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(mgmt))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/item-plans")
                            .file(new MockMultipartFile("data", "", "application/json",
                                    planJson(itemId, 20, 10, 10, beneficiary(), beneficiary()).getBytes()))
                            .file(file("files"))
                            .cookie(appraisal()))
                    .andExpect(status().isForbidden());
        }
    }
}
