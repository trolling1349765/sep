package fpt.capstone.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * Severity of a system-log entry, derived from the action code instead of a
 * stored column so historical rows and future actions classify without
 * migration. Unknown or null actions default to INFO.
 */
public enum LogSeverity {

    INFO, WARNING, CRITICAL;

    private static final Set<String> CRITICAL_ACTIONS = Set.of(
            "RESTORE_START", "RESTORE_COMPLETE", "RESTORE_FAILED", "BACKUP_FAILED",
            "ILLEGAL_REQUEST", "DELETE_USER", "APPLICATION_DELETE",
            "ADDITIONAL_DOCUMENT_DELETE", "USER_DEACTIVATE");

    private static final Set<String> WARNING_ACTIONS = Set.of(
            "CHANGE_ROLE", "PERMISSION_UPDATE", "RIGHT_CREATE", "RIGHT_UPDATE",
            "CREATE_RIGHT", "PASSWORD_RESET", "CHANGE_PASSWORD", "USER_ACTIVATE",
            "SUPPORT_REQUEST_STATUS_UPDATE");

    public static LogSeverity of(String action) {
        if (action == null) {
            return INFO;
        }
        if (CRITICAL_ACTIONS.contains(action)) {
            return CRITICAL;
        }
        if (WARNING_ACTIONS.contains(action)) {
            return WARNING;
        }
        return INFO;
    }

    public static Set<String> criticalActions() {
        return CRITICAL_ACTIONS;
    }

    public static Set<String> warningActions() {
        return WARNING_ACTIONS;
    }

    /** Actions of the two closed severities; INFO is everything outside this set. */
    public static Set<String> nonInfoActions() {
        Set<String> union = new HashSet<>(CRITICAL_ACTIONS);
        union.addAll(WARNING_ACTIONS);
        return union;
    }
}
