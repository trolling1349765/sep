package fpt.capstone.controller;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.request.UpdateSponsorStatusRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SponsorDetailResponse;
import fpt.capstone.dto.response.SponsorListResponse;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import fpt.capstone.service.SponsorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Sponsor (Nhà tài trợ) management — UC 5.7.1–5.7.3. Add/Read/Update only;
 * BR-55 forbids deletion, so "removing" a sponsor is a status change to INACTIVE.
 */
@RestController
@RequestMapping("/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping
    @PreAuthorize("hasAuthority('SPONSOR_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<SponsorListResponse>>> getSponsors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SponsorType type,
            @RequestParam(required = false) SponsorStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                sponsorService.search(page, size, q, type, status, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SPONSOR_VIEW')")
    public ResponseEntity<APIResponse<SponsorDetailResponse>> getSponsor(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(sponsorService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SPONSOR_MANAGE')")
    public ResponseEntity<APIResponse<SponsorDetailResponse>> createSponsor(
            @Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Đăng ký thông tin nhà tài trợ thành công.", sponsorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SPONSOR_MANAGE')")
    public ResponseEntity<APIResponse<SponsorDetailResponse>> updateSponsor(
            @PathVariable String id,
            @Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật thông tin nhà tài trợ thành công.", sponsorService.update(id, request)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SPONSOR_MANAGE')")
    public ResponseEntity<APIResponse<SponsorDetailResponse>> changeStatus(
            @PathVariable String id,
            @RequestBody UpdateSponsorStatusRequest request) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật trạng thái nhà tài trợ thành công.", sponsorService.changeStatus(id, request)));
    }
}
