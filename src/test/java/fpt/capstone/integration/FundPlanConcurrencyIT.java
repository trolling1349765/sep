package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Concurrency invariant for approve: two plans each reserving 60% of the same
 * funding, approved in parallel, must resolve to exactly one success (pending += 60%)
 * and one FUNDING_BALANCE_INSUFFICIENT — the pessimistic donation-row lock serializes
 * the check-then-reserve. Runs on the real Testcontainers MySQL so the lock has effect.
 */
class FundPlanConcurrencyIT extends AbstractIntegrationTest {

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
                {"donationId":%d,"programName":"CT %s","amount":60000000,"purpose":"p","expectedDate":"2026-12-31"}
                """.formatted(funding, uniq());
        MvcResult res = mockMvc.perform(multipart("/fund-plans")
                        .file(new MockMultipartFile("data", "", "application/json", json.getBytes()))
                        .file(file())
                        .cookie(mgmt))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.id");
    }

    @Test
    void twoConcurrentApprovals_onlyOneSucceeds() throws Exception {
        Cookie mgmt = management();
        Cookie appr = appraisal();
        int funding = confirmedFunding(mgmt);
        String planA = createPlan(mgmt, funding);
        String planB = createPlan(mgmt, funding);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();

        for (String plan : new String[]{planA, planB}) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    MvcResult res = mockMvc.perform(post("/fund-plans/{id}/approve", plan).cookie(appr)).andReturn();
                    int http = res.getResponse().getStatus();
                    if (http == 200) {
                        success.incrementAndGet();
                    } else {
                        int code = JsonPath.read(res.getResponse().getContentAsString(), "$.code");
                        if (code == 10041) {
                            insufficient.incrementAndGet();
                        }
                    }
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

        assertEquals(1, success.get(), "exactly one approval should succeed");
        assertEquals(1, insufficient.get(), "the loser should get FUNDING_BALANCE_INSUFFICIENT");

        // final balance: 60M reserved, 40M available
        mockMvc.perform(get("/fundings/{id}", funding).cookie(mgmt))
                .andExpect(jsonPath("$.data.pendingAmount").value(60000000))
                .andExpect(jsonPath("$.data.availableAmount").value(40000000));
    }
}
