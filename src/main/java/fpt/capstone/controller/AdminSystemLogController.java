package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Audit-log viewer endpoint, guarded by AUDIT_LOG_VIEW. */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminSystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ResponseEntity<APIResponse<List<SystemLog>>> getLogs() {
        return ResponseEntity.ok(APIResponse.success(systemLogService.read()));
    }
}
