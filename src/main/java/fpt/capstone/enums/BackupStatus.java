package fpt.capstone.enums;

/** CORRUPTED is set when a restore/verify recomputes a checksum that no longer matches. */
public enum BackupStatus {
    RUNNING, COMPLETED, FAILED, CORRUPTED
}
