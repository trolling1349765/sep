package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Warehouse inbound (UC 5.7.7–5.7.11): item quick-create + dedupe, receipt DRAFT→POSTED
 * (adds on-hand, locks after post), adjustments (append-only, non-negative), the merged
 * ledger, and the concurrency invariant that two posts of the same item sum correctly.
 */
class WarehouseInboundIT extends AbstractIntegrationTest {

    private Cookie management() throws Exception {
        return cookieOf(registerUserWithRole("Management"), "access_token");
    }

    private Cookie citizen() throws Exception {
        return cookieOf(registerUserWithRole("Citizen"), "access_token");
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("files", "e.pdf", "application/pdf", "x".getBytes());
    }

    private String createItem(Cookie mgmt, String name) throws Exception {
        MvcResult res = mockMvc.perform(post("/support-items").cookie(mgmt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"unit\":\"kg\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.startsWith("VP-")))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private String receiptJson(String itemId, int qty) {
        return """
                {"itemId":"%s","quantity":%d,"condition":"NEW","receiveDate":"2026-01-01","evidenceName":"BB"}
                """.formatted(itemId, qty);
    }

    private String createReceipt(Cookie mgmt, String itemId, int qty) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/inbound-receipts")
                        .file(new MockMultipartFile("data", "", "application/json", receiptJson(itemId, qty).getBytes()))
                        .file(file())
                        .cookie(mgmt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    private int onHand(Cookie mgmt, String name) throws Exception {
        MvcResult res = mockMvc.perform(get("/support-items").cookie(mgmt).param("q", name))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.items[0].quantityOnHand");
    }

    @Nested
    @DisplayName("catalog + receipt lifecycle")
    class Lifecycle {

        @Test
        void quickCreate_duplicate_shouldReturn10046() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            createItem(mgmt, name);
            mockMvc.perform(post("/support-items").cookie(mgmt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name.toUpperCase() + "\",\"unit\":\"kg\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10046));
        }

        @Test
        void receipt_draftPostLock_andBalance() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = createItem(mgmt, name);
            String receipt = createReceipt(mgmt, itemId, 1000);

            // create must not touch stock
            assertEquals(0, onHand(mgmt, name));

            // post adds on-hand
            mockMvc.perform(post("/inbound-receipts/{id}/post", receipt).cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("POSTED"));
            assertEquals(1000, onHand(mgmt, name));

            // second post is blocked
            mockMvc.perform(post("/inbound-receipts/{id}/post", receipt).cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10048));

            // editing a POSTED receipt is locked
            mockMvc.perform(multipart("/inbound-receipts/{id}", receipt)
                            .file(new MockMultipartFile("data", "", "application/json", receiptJson(itemId, 5).getBytes()))
                            .with(r -> {
                                r.setMethod("PUT");
                                return r;
                            })
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10051));
        }

        @Test
        void create_missingEvidence_shouldReturn10038() throws Exception {
            Cookie mgmt = management();
            String itemId = createItem(mgmt, "Gao " + uniq());
            mockMvc.perform(multipart("/inbound-receipts")
                            .file(new MockMultipartFile("data", "", "application/json", receiptJson(itemId, 100).getBytes()))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10038));
        }
    }

    @Nested
    @DisplayName("adjustments + ledger")
    class Adjustments {

        @Test
        void adjust_thenLedger_shouldReflectMovements() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = createItem(mgmt, name);
            String receipt = createReceipt(mgmt, itemId, 1000);
            mockMvc.perform(post("/inbound-receipts/{id}/post", receipt).cookie(mgmt)).andExpect(status().isOk());

            // negative adjustment down to 995
            mockMvc.perform(multipart("/inventory/{itemId}/adjustments", itemId)
                            .file(new MockMultipartFile("data", "", "application/json",
                                    "{\"deltaQty\":-5,\"reason\":\"hao hut\"}".getBytes()))
                            .cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balanceAfter").value(995));
            assertEquals(995, onHand(mgmt, name));

            // over-draw is rejected
            mockMvc.perform(multipart("/inventory/{itemId}/adjustments", itemId)
                            .file(new MockMultipartFile("data", "", "application/json",
                                    "{\"deltaQty\":-9999,\"reason\":\"x\"}".getBytes()))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10047));

            // ledger has the inbound (+1000) and the adjustment (-5), newest first
            mockMvc.perform(get("/inventory/{itemId}/transactions", itemId).cookie(mgmt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.items[0].type").value("ADJUSTMENT"))
                    .andExpect(jsonPath("$.data.items[1].type").value("INBOUND"));
        }

        @Test
        void adjust_zeroDelta_shouldReturn10010() throws Exception {
            Cookie mgmt = management();
            String itemId = createItem(mgmt, "Gao " + uniq());
            mockMvc.perform(multipart("/inventory/{itemId}/adjustments", itemId)
                            .file(new MockMultipartFile("data", "", "application/json",
                                    "{\"deltaQty\":0,\"reason\":\"x\"}".getBytes()))
                            .cookie(mgmt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(10010));
        }
    }

    @Nested
    @DisplayName("concurrency + rbac")
    class ConcurrencyAndRbac {

        @Test
        void twoConcurrentPosts_sameItem_shouldSumOnHand() throws Exception {
            Cookie mgmt = management();
            String name = "Gao " + uniq();
            String itemId = createItem(mgmt, name);
            String r1 = createReceipt(mgmt, itemId, 1000);
            String r2 = createReceipt(mgmt, itemId, 1000);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            for (String r : new String[]{r1, r2}) {
                pool.submit(() -> {
                    try {
                        ready.countDown();
                        go.await();
                        mockMvc.perform(post("/inbound-receipts/{id}/post", r).cookie(mgmt))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);

            assertEquals(2000, onHand(mgmt, name));
        }

        @Test
        void citizen_cannotCreateItemOrReceipt() throws Exception {
            Cookie citizen = citizen();
            mockMvc.perform(post("/support-items").cookie(citizen)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"X\",\"unit\":\"kg\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/inbound-receipts")
                            .file(new MockMultipartFile("data", "", "application/json", receiptJson("x", 1).getBytes()))
                            .file(file())
                            .cookie(citizen))
                    .andExpect(status().isForbidden());
        }
    }
}
