package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Audit-log viewer endpoint, guarded by AUDIT_LOG_VIEW. Read-only. */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminSystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ResponseEntity<APIResponse<PageResponse<SystemLogResponse>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(APIResponse.success(
                systemLogService.search(action, entityType, userId, from, to, page, size)));
    }
}
