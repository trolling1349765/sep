package fpt.capstone.controller;

import fpt.capstone.dto.request.FundingCreateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.FundingDetailResponse;
import fpt.capstone.dto.response.FundingListResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.service.FundingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Funding sources (Nguon kinh phi) — UC 5.7.4–5.7.6. Recording money requires
 * evidence: with files the record is CONFIRMED, otherwise DRAFT. No delete.
 *
 * <p>Multipart convention (reused across the donor module): a JSON part "data"
 * holds the DTO, an optional repeated part "files" carries attachments.
 */
@RestController
@RequestMapping("/fundings")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @GetMapping
    @PreAuthorize("hasAuthority('FUNDING_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<FundingListResponse>>> getFundings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FundingStatus status,
            @RequestParam(required = false) String sponsorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(
                fundingService.search(page, size, status, sponsorId, fromDate, toDate, sort, dir)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FUNDING_VIEW')")
    public ResponseEntity<APIResponse<FundingDetailResponse>> getFunding(@PathVariable int id) {
        return ResponseEntity.ok(APIResponse.success(fundingService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FUNDING_RECORD_CREATE')")
    public ResponseEntity<APIResponse<FundingDetailResponse>> createFunding(
            @Valid @RequestPart("data") FundingCreateRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Ghi nhận nguồn kinh phí thành công.", fundingService.create(data, files)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FUNDING_RECORD_CREATE')")
    public ResponseEntity<APIResponse<FundingDetailResponse>> updateFunding(
            @PathVariable int id,
            @Valid @RequestPart("data") FundingCreateRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cập nhật nguồn kinh phí thành công.", fundingService.update(id, data, files)));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('FUNDING_MANAGE')")
    public ResponseEntity<APIResponse<FundingDetailResponse>> confirmFunding(@PathVariable int id) {
        return ResponseEntity.ok(APIResponse.success(
                "Xác nhận nguồn kinh phí thành công.", fundingService.confirm(id)));
    }
}
