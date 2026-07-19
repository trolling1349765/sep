package fpt.capstone.controller;

import fpt.capstone.dto.request.BackupCreateRequest;
import fpt.capstone.dto.request.RestoreRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BackupOverviewResponse;
import fpt.capstone.dto.response.BackupResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.RestoreResultResponse;
import fpt.capstone.service.BackupService;
import fpt.capstone.service.RestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backup/restore admin endpoints. Backups append to the ledger only — there is
 * deliberately no update or delete endpoint (append-only, like the audit log).
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupService backupService;
    private final RestoreService restoreService;

    @GetMapping("/backups/overview")
    @PreAuthorize("hasAuthority('BACKUP_MANAGE')")
    public ResponseEntity<APIResponse<BackupOverviewResponse>> overview() {
        return ResponseEntity.ok(APIResponse.success(backupService.overview()));
    }

    @PostMapping("/backups")
    @PreAuthorize("hasAuthority('BACKUP_MANAGE')")
    public ResponseEntity<APIResponse<BackupResponse>> create(@RequestBody BackupCreateRequest request) {
        return ResponseEntity.ok(APIResponse.success(backupService.create(request.getType())));
    }

    @GetMapping("/backups")
    @PreAuthorize("hasAuthority('BACKUP_MANAGE')")
    public ResponseEntity<APIResponse<PageResponse<BackupResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(APIResponse.success(backupService.list(page, size, status, type)));
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('RESTORE_MANAGE')")
    public ResponseEntity<APIResponse<RestoreResultResponse>> restore(@RequestBody RestoreRequest request) {
        return ResponseEntity.ok(APIResponse.success(restoreService.restore(request)));
    }
}
