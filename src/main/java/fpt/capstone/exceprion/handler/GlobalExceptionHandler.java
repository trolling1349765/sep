package fpt.capstone.exceprion.handler;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<List<APIResponse>> handlingRuntimeException(AppException e) {
        return ResponseEntity.badRequest().body(e.getResponses());
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = ArgumentNotValidException.class)
    ResponseEntity<List<APIResponse>> handlingMethodArgumentNotValidException(ArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(e.getResponses());
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        APIResponse<Map<String, Object>> response = new APIResponse<>();
        FieldError fieldError = e.getBindingResult().getFieldError();
        String defaultMessage = fieldError != null ? fieldError.getDefaultMessage() : null;

        // Some DTOs use ErrorCode enum names as validation messages, others plain text
        ErrorCode errorCode = null;
        if (defaultMessage != null) {
            try {
                errorCode = ErrorCode.valueOf(defaultMessage);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (errorCode != null) {
            response.setMessage(errorCode.getMessage());
            response.setCode(errorCode.getCode());
        } else {
            response.setMessage(defaultMessage != null ? defaultMessage : "Invalid request.");
            response.setCode(400);
        }

        if (fieldError != null) {
            Map<String, Object> data = new HashMap<>();
            data.put(fieldError.getField(), fieldError.getRejectedValue());
            response.setData(data);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = InvalidArgsException.class)
    ResponseEntity<APIResponse> handlingInvalidArgsException(InvalidArgsException e) {
        return ResponseEntity.badRequest().body(e.getResponse());
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = DataAccessException.class)
    ResponseEntity<APIResponse> handleDataAccessException(DataAccessException e) {
        log.error("Data access failure", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(APIResponse.error(503, "Service temporarily unavailable. Please try again later."));
    }

    @ExceptionHandler(value = ResponseStatusException.class)
    ResponseEntity<APIResponse<Void>> handleResponseStatusException(
            ResponseStatusException e, HttpServletResponse servletResponse) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        if (status == HttpStatus.TOO_MANY_REQUESTS && e.getHeaders() != null) {
            String retryAfter = e.getHeaders().getFirst("Retry-After");
            if (retryAfter != null) {
                servletResponse.setHeader("Retry-After", retryAfter);
            }
        }
        // Some services pass an ErrorCode enum name as the reason; map it to the business code/message.
        ErrorCode errorCode = null;
        if (e.getReason() != null) {
            try {
                errorCode = ErrorCode.valueOf(e.getReason());
            } catch (IllegalArgumentException ignored) {
            }
        }
        int code = errorCode != null ? errorCode.getCode() : status.value();
        String message = errorCode != null ? errorCode.getMessage() : e.getReason();
        return ResponseEntity.status(status).body(APIResponse.error(code, message));
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<APIResponse<Void>> handleUnexpectedException(Exception e) throws Exception {
        // Let Spring Security translate authorization failures into 401/403
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        // Preserve framework status semantics (404 NoResourceFound, 405, 415, ...)
        if (e instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            return ResponseEntity.status(status)
                    .body(APIResponse.error(status, HttpStatus.valueOf(status).getReasonPhrase()));
        }
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.error(500, "An unexpected error occurred. Please try again later."));
    }
}
