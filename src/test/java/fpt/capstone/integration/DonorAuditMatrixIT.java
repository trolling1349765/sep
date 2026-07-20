package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.repository.BenificiaryRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Audit matrix (Đợt 6 §4.1): drives every donor-module write once through the whole
 * money + goods flow and asserts all 27 audit actions land in system_log. Doubles as the
 * end-to-end acceptance script (create sponsor → funding → plans → warehouse → distribute).
 */
class DonorAuditMatrixIT extends AbstractIntegrationTest {

    @Autowired
    private BenificiaryRepository benificiaryRepository;

    private Cookie mgmt;
    private Cookie appr;

    private MockMultipartFile file(String field) {
        return new MockMultipartFile(field, "f.pdf", "application/pdf", "x".getBytes());
    }

    private MockMultipartFile data(String json) {
        return new MockMultipartFile("data", "", "application/json", json.getBytes());
    }

    private String read(MvcResult res, String path) throws Exception {
        return JsonPath.read(res.getResponse().getContentAsString(), path).toString();
    }

    private MvcResult ok(RequestBuilder rb) throws Exception {
        return mockMvc.perform(rb).andExpect(status().isOk()).andReturn();
    }

    private int beneficiary() {
        return benificiaryRepository.save(Benificiary.builder().fullName("Ben " + uniq()).build()).getId();
    }

    @Test
    void everyWrite_shouldEmitAllTwentySevenAuditActions() throws Exception {
        mgmt = cookieOf(registerUserWithRole("Management"), "access_token");
        appr = cookieOf(registerUserWithRole("Appraisal"), "access_token");

        sponsorFlow();
        int fundingId = fundingFlow();
        fundPlanFlow(fundingId);
        String itemId = warehouseFlow();
        itemPlanAndDistributionFlow(itemId);

        Set<String> actions = systemLogRepository.findAll().stream()
                .map(l -> l.getAction()).collect(Collectors.toSet());

        String[] expected = {
                "SPONSOR_CREATE", "SPONSOR_UPDATE", "SPONSOR_STATUS_CHANGE",
                "FUNDING_CREATE", "FUNDING_UPDATE", "FUNDING_CONFIRM",
                "FUND_PLAN_CREATE", "FUND_PLAN_UPDATE", "FUND_PLAN_DELETE", "FUND_PLAN_APPROVE",
                "FUND_PLAN_REJECT", "FUND_PLAN_CANCEL", "FUND_PLAN_COMPLETE",
                "SUPPORT_ITEM_CREATE", "RECEIPT_CREATE", "RECEIPT_UPDATE", "RECEIPT_POST", "STOCK_ADJUST",
                "ITEM_PLAN_CREATE", "ITEM_PLAN_UPDATE", "ITEM_PLAN_DELETE", "ITEM_PLAN_APPROVE",
                "ITEM_PLAN_REJECT", "ITEM_PLAN_CANCEL",
                "DISTRIBUTION_CREATE", "DISTRIBUTION_CONFIRM", "DISTRIBUTION_NOT_DELIVERED"
        };
        for (String action : expected) {
            assertTrue(actions.contains(action), "Missing audit action: " + action);
        }
    }

    private void sponsorFlow() throws Exception {
        String body = """
                {"name":"Quy %s","type":"ORG","orgCode":"ORG-%s","contactPerson":"A",
                 "phone":"%s","email":"%s","address":"HN"}
                """.formatted(uniq(), uniq(), uniquePhone(), uniqueEmail());
        MvcResult created = ok(post("/sponsors").cookie(mgmt).contentType(MediaType.APPLICATION_JSON).content(body));
        String id = read(created, "$.data.id");
        String update = """
                {"name":"Quy %s","type":"ORG","orgCode":"ORG-%s","contactPerson":"B",
                 "phone":"%s","email":"%s","address":"HCM"}
                """.formatted(uniq(), uniq(), uniquePhone(), uniqueEmail());
        ok(put("/sponsors/{id}", id).cookie(mgmt).contentType(MediaType.APPLICATION_JSON).content(update));
        ok(put("/sponsors/{id}/status", id).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"));
    }

    private int fundingFlow() throws Exception {
        String create = """
                {"name":"KP %s","amount":100000000,"receivedDate":"2026-01-01","purpose":"p",
                 "paymentMethod":"CASH","evidenceName":"BB"}
                """.formatted(uniq());
        // create without files -> DRAFT
        MvcResult created = ok(multipart("/fundings").file(data(create)).cookie(mgmt));
        int id = Integer.parseInt(read(created, "$.data.id"));
        // update (still DRAFT) appends the evidence file
        ok(multipart("/fundings/" + id).file(data(create)).file(file("files"))
                .with(r -> { r.setMethod("PUT"); return r; }).cookie(mgmt));
        // confirm -> CONFIRMED
        ok(put("/fundings/{id}/confirm", id).cookie(mgmt));
        return id;
    }

    private void fundPlanFlow(int fundingId) throws Exception {
        // reject branch
        String pReject = createFundPlan(fundingId, 5000000);
        ok(post("/fund-plans/{id}/reject", pReject).cookie(appr)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"thieu\"}"));
        // update branch (rejected -> resubmit)
        ok(multipart("/fund-plans/" + pReject).file(data(fundPlanJson(fundingId, 5000000)))
                .with(r -> { r.setMethod("PUT"); return r; }).cookie(mgmt));
        // delete branch
        String pDelete = createFundPlan(fundingId, 5000000);
        ok(post("/fund-plans/{id}/delete", pDelete).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"nham\"}"));
        // approve + cancel
        String pCancel = createFundPlan(fundingId, 10000000);
        ok(post("/fund-plans/{id}/approve", pCancel).cookie(appr));
        ok(post("/fund-plans/{id}/cancel", pCancel).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"huy\"}"));
        // approve + complete
        String pComplete = createFundPlan(fundingId, 10000000);
        ok(post("/fund-plans/{id}/approve", pComplete).cookie(appr));
        ok(multipart("/fund-plans/" + pComplete + "/complete").file(file("files")).cookie(mgmt));
    }

    private String fundPlanJson(int fundingId, int amount) {
        return """
                {"donationId":%d,"programName":"CT %s","amount":%d,"purpose":"p","expectedDate":"2026-12-31"}
                """.formatted(fundingId, uniq(), amount);
    }

    private String createFundPlan(int fundingId, int amount) throws Exception {
        MvcResult res = ok(multipart("/fund-plans").file(data(fundPlanJson(fundingId, amount)))
                .file(file("files")).cookie(mgmt));
        return read(res, "$.data.id");
    }

    private String warehouseFlow() throws Exception {
        MvcResult item = ok(post("/support-items").cookie(mgmt).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Gao " + uniq() + "\",\"unit\":\"kg\"}"));
        String itemId = read(item, "$.data.id");
        String receiptJson = """
                {"itemId":"%s","quantity":1000,"condition":"NEW","receiveDate":"2026-01-01","evidenceName":"BB"}
                """.formatted(itemId);
        MvcResult receipt = ok(multipart("/inbound-receipts").file(data(receiptJson)).file(file("files")).cookie(mgmt));
        String receiptId = read(receipt, "$.data.id");
        ok(multipart("/inbound-receipts/" + receiptId).file(data(receiptJson))
                .with(r -> { r.setMethod("PUT"); return r; }).cookie(mgmt));
        ok(post("/inbound-receipts/{id}/post", receiptId).cookie(mgmt));
        ok(multipart("/inventory/{itemId}/adjustments", itemId)
                .file(data("{\"deltaQty\":5,\"reason\":\"kiem ke\"}")).cookie(mgmt));
        return itemId;
    }

    private void itemPlanAndDistributionFlow(String itemId) throws Exception {
        // reject + update branch
        String ipReject = createItemPlan(itemId, 20, 10, 10);
        ok(post("/item-plans/{id}/reject", ipReject).cookie(appr)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"sua\"}"));
        ok(multipart("/item-plans/" + ipReject).file(data(itemPlanJson(itemId, 20, 10, 10)))
                .with(r -> { r.setMethod("PUT"); return r; }).cookie(mgmt));
        // delete branch
        String ipDelete = createItemPlan(itemId, 20, 10, 10);
        ok(post("/item-plans/{id}/delete", ipDelete).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"nham\"}"));
        // approve + cancel branch
        String ipCancel = createItemPlan(itemId, 20, 10, 10);
        ok(post("/item-plans/{id}/approve", ipCancel).cookie(appr));
        ok(post("/item-plans/{id}/cancel", ipCancel).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"huy\"}"));
        // approve + distribute + confirm + not-delivered
        String ipMain = createItemPlan(itemId, 20, 10, 10);
        ok(post("/item-plans/{id}/approve", ipMain).cookie(appr));
        MvcResult detail = ok(get("/item-plans/{id}", ipMain).cookie(mgmt));
        String l1 = read(detail, "$.data.lines[0].id");
        String l2 = read(detail, "$.data.lines[1].id");
        String distJson = """
                {"planLineId":"%s","actualQty":10,"issueDate":"2026-02-01","recipientName":"An"}
                """.formatted(l1);
        MvcResult dist = ok(multipart("/distributions").file(data(distJson)).file(file("files")).cookie(mgmt));
        String distId = read(dist, "$.data.id");
        ok(post("/distributions/{id}/confirm", distId).cookie(mgmt));
        ok(post("/item-plans/lines/{lineId}/not-delivered", l2).cookie(mgmt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"vang\",\"returnStock\":true}"));
    }

    private String itemPlanJson(String itemId, int total, int q1, int q2) {
        return """
                {"itemId":"%s","plannedQty":%d,"expectedDate":"2026-12-31","deliveryPlace":"UBND",
                 "lines":[{"beneficiaryId":%d,"plannedQty":%d},{"beneficiaryId":%d,"plannedQty":%d}]}
                """.formatted(itemId, total, beneficiary(), q1, beneficiary(), q2);
    }

    private String createItemPlan(String itemId, int total, int q1, int q2) throws Exception {
        MvcResult res = ok(multipart("/item-plans").file(data(itemPlanJson(itemId, total, q1, q2)))
                .file(file("files")).cookie(mgmt));
        return read(res, "$.data.id");
    }
}
