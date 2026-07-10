package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.service.DonationService;
import fpt.capstone.service.SponsorService;
import fpt.capstone.service.GoodsInventoryService;
import fpt.capstone.service.DistributionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/donation-management")
@RequiredArgsConstructor
public class DonationManagementController {

    private final SponsorService sponsorService;
    private final DonationService donationService;
    private final GoodsInventoryService inventoryService;
    private final DistributionRecordService distributionService;

    @GetMapping("/dashboard")
    public ResponseEntity<APIResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalSponsors", sponsorService.getAllSponsors().size());
        dashboard.put("totalDonations", donationService.getAllDonations().size());
        dashboard.put("totalInventoryItems", inventoryService.getAllInventories().size());
        dashboard.put("totalPlans", distributionService.getAllAllocationPlans().size());
        dashboard.put("totalDistributions", distributionService.getAllDistributions().size());
        return ResponseEntity.ok(APIResponse.success(dashboard));
    }
}