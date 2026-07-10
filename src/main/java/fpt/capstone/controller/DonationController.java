package fpt.capstone.controller;

import fpt.capstone.dto.request.DonationRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DonationResponse;
import fpt.capstone.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<APIResponse<DonationResponse>> createDonation(@Valid @RequestBody DonationRequest request) {
        DonationResponse response = donationService.createDonation(request);
        return ResponseEntity.ok(APIResponse.success("Ghi nhận nguồn kinh phí thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<DonationResponse>> getDonationById(@PathVariable int id) {
        DonationResponse response = donationService.getDonationById(id);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<DonationResponse>>> getAllDonations(
            @RequestParam(required = false) String sponsorId) {
        List<DonationResponse> responses;
        if (sponsorId != null) {
            responses = donationService.getDonationsBySponsor(sponsorId);
        } else {
            responses = donationService.getAllDonations();
        }
        return ResponseEntity.ok(APIResponse.success(responses));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteDonation(@PathVariable int id) {
        donationService.deleteDonation(id);
        return ResponseEntity.ok(APIResponse.success("Xóa khoản tài trợ thành công", null));
    }
}