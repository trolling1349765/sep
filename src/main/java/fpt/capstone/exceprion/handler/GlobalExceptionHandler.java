package fpt.capstone.exceprion.handler;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.ArgumentNotValidException;
import fpt.capstone.exceprion.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<List<APIResponse>> handlingRuntimeException(AppException e) {
        return ResponseEntity.badRequest().body(e.getResponses());// cause by user
    }


    @ExceptionHandler(value = ArgumentNotValidException.class)
    ResponseEntity<List<APIResponse>> handlingMethodArgumentNotValidException(ArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(e.getResponses());// validation exception
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        APIResponse response = new APIResponse();
        Map<String, Object> data = new HashMap<>();
        ErrorCode errorCode = ErrorCode.valueOf(e.getBindingResult().getFieldError().getDefaultMessage());

        response.setMessage(errorCode.getMessage());
        response.setCode(errorCode.getCode());
        data.put(e.getBindingResult().getFieldError().getField(), e.getBindingResult().getFieldError().getRejectedValue());
        response.setData(data);
        return ResponseEntity.badRequest().body(response);
    }
}
