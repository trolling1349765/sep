package fpt.capstone.enums;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSeverityTest {

    @Test
    void of_everyCriticalAction_returnsCritical() {
        for (String action : Set.of("RESTORE_START", "RESTORE_COMPLETE", "RESTORE_FAILED",
                "BACKUP_FAILED", "ILLEGAL_REQUEST", "DELETE_USER", "APPLICATION_DELETE",
                "ADDITIONAL_DOCUMENT_DELETE", "USER_DEACTIVATE")) {
            assertEquals(LogSeverity.CRITICAL, LogSeverity.of(action), action);
        }
    }

    @Test
    void of_everyWarningAction_returnsWarning() {
        for (String action : Set.of("CHANGE_ROLE", "PERMISSION_UPDATE", "RIGHT_CREATE",
                "RIGHT_UPDATE", "CREATE_RIGHT", "PASSWORD_RESET", "CHANGE_PASSWORD",
                "USER_ACTIVATE", "SUPPORT_REQUEST_STATUS_UPDATE")) {
            assertEquals(LogSeverity.WARNING, LogSeverity.of(action), action);
        }
    }

    @Test
    void of_regularActions_returnInfo() {
        assertEquals(LogSeverity.INFO, LogSeverity.of("USER_LOGIN"));
        assertEquals(LogSeverity.INFO, LogSeverity.of("BACKUP_CREATE"));
        assertEquals(LogSeverity.INFO, LogSeverity.of("BACKUP_COMPLETE"));
        assertEquals(LogSeverity.INFO, LogSeverity.of("CREATE_APPLICATION"));
    }

    @Test
    void of_unknownAction_returnsInfo() {
        assertEquals(LogSeverity.INFO, LogSeverity.of("SOME_LEGACY_ACTION"));
    }

    @Test
    void of_null_returnsInfo() {
        assertEquals(LogSeverity.INFO, LogSeverity.of(null));
    }

    @Test
    void nonInfoActions_isUnionOfCriticalAndWarning() {
        Set<String> expected = new HashSet<>(LogSeverity.criticalActions());
        expected.addAll(LogSeverity.warningActions());

        assertEquals(expected, LogSeverity.nonInfoActions());
        assertEquals(18, LogSeverity.nonInfoActions().size());
    }

    @Test
    void criticalAndWarning_areDisjoint() {
        Set<String> overlap = new HashSet<>(LogSeverity.criticalActions());
        overlap.retainAll(LogSeverity.warningActions());
        assertTrue(overlap.isEmpty());
    }
}
