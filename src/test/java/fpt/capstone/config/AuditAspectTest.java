package fpt.capstone.config;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private SystemLogService systemLogService;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private JoinPoint joinPoint;

    @InjectMocks
    private AuditAspect auditAspect;

    private Auditable auditable(Action action, Table table) {
        return new Auditable() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Auditable.class;
            }

            @Override
            public Action action() {
                return action;
            }

            @Override
            public Table entity() {
                return table;
            }
        };
    }

    public static class ResultWithId {
        public String getId() {
            return "record-7";
        }
    }

    @Test
    @DisplayName("audit_shouldWriteLogWithActorActionAndEntityId")
    void audit_shouldWriteLogWithActorActionAndEntityId() {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");

        auditAspect.audit(joinPoint, auditable(Action.SUPPORT_REQUEST_CREATE, Table.SUPPORT_REQUEST),
                new ResultWithId());

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).write(captor.capture());
        SystemLog written = captor.getValue();
        assertEquals("SUPPORT_REQUEST_CREATE", written.getAction());
        assertEquals("SUPPORT_REQUESTS", written.getEntityType());
        assertEquals("user-1", written.getUserId());
        assertEquals("record-7", written.getEntityId());
        assertNotNull(written.getCreatedAt());
    }

    @Test
    @DisplayName("audit_shouldSwallowLoggingFailures")
    void audit_shouldSwallowLoggingFailures() {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(systemLogService.write(any())).thenThrow(new RuntimeException("DB down"));
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);

        assertDoesNotThrow(() -> auditAspect.audit(joinPoint,
                auditable(Action.SUPPORT_REQUEST_CREATE, Table.SUPPORT_REQUEST), null));
    }

    @Test
    @DisplayName("audit_shouldTolerateMissingActorAndResult")
    void audit_shouldTolerateMissingActorAndResult() {
        when(securityUtil.getCurrentUserId()).thenThrow(new RuntimeException("no request bound"));

        assertDoesNotThrow(() -> auditAspect.audit(joinPoint,
                auditable(Action.SUPPORT_REQUEST_REPLY, Table.SUPPORT_REQUEST), "plain-string-result"));

        // String has no getId() -> entityId null; actor lookup failure -> userId null
        verify(systemLogService).write(argThat(log ->
                log.getEntityId() == null && log.getUserId() == null));
    }
}
