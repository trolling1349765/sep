package fpt.capstone.controller;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.SponsorResponse;
import fpt.capstone.service.SponsorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @PostMapping
    public ResponseEntity<APIResponse<SponsorResponse>> createSponsor(@Valid @RequestBody SponsorRequest request) {
        SponsorResponse response = sponsorService.createSponsor(request);
        return ResponseEntity.ok(APIResponse.success("Đăng ký thông tin nhà tài trợ thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<SponsorResponse>> updateSponsor(
            @PathVariable String id,
            @Valid @RequestBody SponsorRequest request) {
        SponsorResponse response = sponsorService.updateSponsor(id, request);
        return ResponseEntity.ok(APIResponse.success("Cập nhật thông tin nhà tài trợ thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SponsorResponse>> getSponsorById(@PathVariable String id) {
        SponsorResponse response = sponsorService.getSponsorById(id);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<SponsorResponse>>> getAllSponsors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        List<SponsorResponse> responses;
        if (keyword != null) {
            responses = sponsorService.searchSponsors(keyword);
        } else if (type != null) {
            responses = sponsorService.getSponsorsByType(type);
        } else {
            responses = sponsorService.getAllSponsors();
        }
        return ResponseEntity.ok(APIResponse.success(responses));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteSponsor(@PathVariable String id) {
        sponsorService.deleteSponsor(id);
        return ResponseEntity.ok(APIResponse.success("Xóa nhà tài trợ thành công", null));
    }
}