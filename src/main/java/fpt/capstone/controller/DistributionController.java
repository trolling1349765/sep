package fpt.capstone.controller;

import fpt.capstone.dto.request.DistributionCreateRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DistributionResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.DistributionStatus;
import fpt.capstone.service.DistributionService;
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

/** Distribution (Cap phat) + history (Lich su) — UC 5.7.13/5.7.14. History is append-only. */
@RestController
@RequestMapping("/distributions")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_HISTORY_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<DistributionResponse>>> getDistributions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) Integer beneficiaryId,
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) DistributionStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(APIResponse.success(distributionService.search(
                page, size, itemId, beneficiaryId, planId, fromDate, toDate, status, sort, dir)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ITEM_DISTRIBUTE')")
    public ResponseEntity<APIResponse<DistributionResponse>> createDistribution(
            @Valid @RequestPart("data") DistributionCreateRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(APIResponse.success(
                "Cấp phát vật phẩm thành công.", distributionService.create(data, files)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('DISTRIBUTION_CONFIRM')")
    public ResponseEntity<APIResponse<DistributionResponse>> confirmDistribution(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.success(
                "Công dân đã xác nhận nhận vật phẩm.", distributionService.confirm(id)));
    }
}
