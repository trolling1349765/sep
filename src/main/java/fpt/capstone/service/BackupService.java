package fpt.capstone.service;

import fpt.capstone.dto.response.BackupOverviewResponse;
import fpt.capstone.dto.response.BackupResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.enums.BackupType;

public interface BackupService {

    /**
     * Runs a synchronous backup. createdBy is the acting admin's user id, or
     * null when invoked by the scheduler. Throws 409 BACKUP_IN_PROGRESS when
     * another backup is already running.
     */
    BackupResponse create(BackupType type, String createdBy);

    /** Parses and validates the raw type string, then delegates to create. */
    BackupResponse create(String type);

    PageResponse<BackupResponse> list(int page, int size, String status, String type);

    BackupOverviewResponse overview();
}
