package fpt.capstone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.service.SystemLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditingAccessDeniedHandlerTest {

    @Mock
    private SystemLogService systemLogService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AuditingAccessDeniedHandler handler() {
        return new AuditingAccessDeniedHandler(systemLogService, new ObjectMapper());
    }

    @Test
    @DisplayName("handle_shouldWriteIllegalRequestLogAndReturn403Body")
    void handle_shouldWriteIllegalRequestLogAndReturn403Body() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/capstone/applications");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler().handle(request, response, new AccessDeniedException("denied"));

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogService).write(captor.capture());
        assertEquals("ILLEGAL_REQUEST", captor.getValue().getAction());
        assertEquals("GET /capstone/applications", captor.getValue().getEntityId());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(ErrorCode.ACCESS_DENIED.getCode())));
    }

    @Test
    @DisplayName("handle_shouldStillReturn403WhenLoggingFails")
    void handle_shouldStillReturn403WhenLoggingFails() throws Exception {
        when(systemLogService.write(any())).thenThrow(new RuntimeException("DB down"));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/capstone/benificiary");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> handler().handle(request, response, new AccessDeniedException("denied")));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertFalse(response.getContentAsString().isBlank());
    }
}
