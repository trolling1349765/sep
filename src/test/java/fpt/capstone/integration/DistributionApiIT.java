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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Distribution end-to-end (UC 5.7.13/5.7.14): approve reserves, distribute draws BOTH
 * on-hand and reserved (available unchanged), confirm, per-line not-delivered releases the
 * hold, and the header auto-COMPLETEs when no PENDING line remains. Verifies the stock
 * invariant available = onHand - reserved after each step.
 */
class DistributionApiIT extends AbstractIntegrationTest {

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

    private String itemWithStock(Cookie mgmt, String name, int qty) throws Exception {
        MvcResult item = mockMvc.perform(post("/support-items").cookie(mgmt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"unit\":\"kg\"}"))
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

    private String createPlan(Cookie mgmt, String itemId, int total, int q1, int q2) throws Exception {
        String json = """
                {"itemId":"%s","plannedQty":%d,"expectedDate":"2026-12-31","deliveryPlace":"UBND",
                 "lines":[{"beneficiaryId":%d,"plannedQty":%d},{"beneficiaryId":%d,"plannedQty":%d}]}
                """.formatted(itemId, total, beneficiary(), q1, beneficiary(), q2);
        MvcResult res = mockMvc.perform(multipart("/item-plans")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private String lineId(Cookie token, String planId, int index) throws Exception {
        MvcResult res = mockMvc.perform(get("/item-plans/{id}", planId).cookie(token))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.lines[" + index + "].id");
    }

    private void assertBalance(Cookie token, String name, int onHand, int reserved, int available) throws Exception {
        mockMvc.perform(get("/support-items").cookie(token).param("q", name))
                .andExpect(jsonPath("$.data.items[0].quantityOnHand").value(onHand))
                .andExpect(jsonPath("$.data.items[0].reservedQuantity").value(reserved))
                .andExpect(jsonPath("$.data.items[0].available").value(available));
    }

    private String distribute(Cookie mgmt, String lineId, int qty) throws Exception {
        String json = """
                {"planLineId":"%s","actualQty":%d,"issueDate":"2026-02-01","recipientName":"Pham An"}
                """.formatted(lineId, qty);
        MvcResult res = mockMvc.perform(multipart("/distributions")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file("files"))
                        .cookie(mgmt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Nested
    @DisplayName("end-to-end + invariants")
    class EndToEnd {

        @Test
        void distributeConfirmAndNotDelivered_shouldCompleteAndKeepInvariant() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = itemWithStock(mgmt, name, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            assertBalance(mgmt, name, 1000, 0, 1000);

            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(appraisal())).andExpect(status().isOk());
            assertBalance(mgmt, name, 1000, 20, 980);

            String l1 = lineId(mgmt, plan, 0);
            String l2 = lineId(mgmt, plan, 1);

            // distribute line 1: both on-hand and reserved drop by 10, available unchanged
            String dist = distribute(mgmt, l1, 10);
            assertBalance(mgmt, name, 990, 10, 980);

            mockMvc.perform(post("/distributions/{id}/confirm", dist).cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("RECIPIENT_CONFIRMED"));

            // line 2 absent, return stock: releases the 10 hold, header auto-COMPLETEs
            mockMvc.perform(post("/item-plans/lines/{lineId}/not-delivered", l2).cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"note\":\"vang mat\",\"returnStock\":true}"))
                    .andExpect(status().isOk());
            assertBalance(mgmt, name, 990, 0, 990);

            mockMvc.perform(get("/item-plans/{id}", plan).cookie(mgmt))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // history has the single distribution
            mockMvc.perform(get("/distributions").cookie(mgmt).param("planId", plan))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void approveThenCancel_shouldReturnReserve() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = itemWithStock(mgmt, name, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(appraisal())).andExpect(status().isOk());
            assertBalance(mgmt, name, 1000, 20, 980);

            mockMvc.perform(post("/item-plans/{id}/cancel", plan).cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"huy\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
            assertBalance(mgmt, name, 1000, 0, 1000);
        }
    }

    @Nested
    @DisplayName("distribute guards")
    class Guards {

        @Test
        void distribute_twice_shouldReturn10056() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = itemWithStock(mgmt, name, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(appraisal())).andExpect(status().isOk());
            String l1 = lineId(mgmt, plan, 0);
            distribute(mgmt, l1, 10);

            String json = """
                    {"planLineId":"%s","actualQty":5,"issueDate":"2026-02-01","recipientName":"X"}
                    """.formatted(l1);
            mockMvc.perform(multipart("/distributions")
                            .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10056));
        }

        @Test
        void distribute_exceedsPlanned_shouldReturn10053() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = itemWithStock(mgmt, name, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            mockMvc.perform(post("/item-plans/{id}/approve", plan).cookie(appraisal())).andExpect(status().isOk());
            String l1 = lineId(mgmt, plan, 0);

            String json = """
                    {"planLineId":"%s","actualQty":15,"issueDate":"2026-02-01","recipientName":"X"}
                    """.formatted(l1);
            mockMvc.perform(multipart("/distributions")
                            .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10053));
        }

        @Test
        void distribute_planNotApproved_shouldReturn10043() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = itemWithStock(mgmt, name, 1000);
            String plan = createPlan(mgmt, itemId, 20, 10, 10);
            String l1 = lineId(mgmt, plan, 0);

            String json = """
                    {"planLineId":"%s","actualQty":5,"issueDate":"2026-02-01","recipientName":"X"}
                    """.formatted(l1);
            mockMvc.perform(multipart("/distributions")
                            .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                            .file(file("files"))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10043));
        }
    }
}
