package fpt.capstone.exceprion.handler;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.exceprion.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> data = new HashMap<>();
        ErrorCode errorCode = ErrorCode.valueOf(e.getBindingResult().getFieldError().getDefaultMessage());

        response.setMessage(errorCode.getMessage());
        response.setCode(errorCode.getCode());
        data.put(e.getBindingResult().getFieldError().getField(),
                e.getBindingResult().getFieldError().getRejectedValue());
        response.setData(data);
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
        return ResponseEntity.status(status)
                .body(APIResponse.error(status.value(), e.getReason()));
    }
}
