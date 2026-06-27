package fpt.capstone.controller;

import fpt.capstone.dto.request.CreateSupportRequest;
import fpt.capstone.dto.request.ReplySupportRequest;
import fpt.capstone.dto.request.UpdateSupportRequestStatus;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.SupportReplyResponse;
import fpt.capstone.dto.response.SupportRequestDetailResponse;
import fpt.capstone.dto.response.SupportRequestListResponse;
import fpt.capstone.entity.User;
import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import fpt.capstone.service.SupportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/support-requests")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse<SupportRequestDetailResponse>> createSupportRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSupportRequest request) {
        SupportRequestDetailResponse response = supportRequestService.createSupportRequest(user, request);
        return ResponseEntity.ok(APIResponse.success("Support request has been sent successfully.", response));
    }

    @GetMapping("/my")
    public ResponseEntity<APIResponse<Page<SupportRequestListResponse>>> getMyRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SupportRequestListResponse> requests = supportRequestService.getMyRequests(user, page, size);
        return ResponseEntity.ok(APIResponse.success(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SupportRequestDetailResponse>> getRequestDetail(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        SupportRequestDetailResponse response = supportRequestService.getRequestDetail(id, user);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('RECEPTION_OFFICER', 'SOCIAL_AFFAIRS_OFFICER')")
    public ResponseEntity<APIResponse<Page<SupportRequestListResponse>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SupportRequestStatus status,
            @RequestParam(required = false) SupportCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        Page<SupportRequestListResponse> requests = supportRequestService.getAllRequests(
                page, size, status, category, dateFrom, dateTo);
        return ResponseEntity.ok(APIResponse.success(requests));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('RECEPTION_OFFICER', 'SOCIAL_AFFAIRS_OFFICER')")
    public ResponseEntity<APIResponse<SupportReplyResponse>> replyToRequest(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody ReplySupportRequest request) {
        SupportReplyResponse response = supportRequestService.replyToRequest(id, user, request);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECEPTION_OFFICER', 'SOCIAL_AFFAIRS_OFFICER')")
    public ResponseEntity<APIResponse<SupportRequestDetailResponse>> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody UpdateSupportRequestStatus request) {
        SupportRequestDetailResponse response = supportRequestService.updateStatus(id, user, request);
        return ResponseEntity.ok(APIResponse.success(response));
    }

    @PostMapping("/upload")
    public ResponseEntity<APIResponse<String>> uploadAttachment(
            @RequestParam("file") MultipartFile file) {
        String attachmentId = supportRequestService.uploadAttachment(file);
        return ResponseEntity.ok(APIResponse.success("File uploaded successfully.", attachmentId));
    }
}