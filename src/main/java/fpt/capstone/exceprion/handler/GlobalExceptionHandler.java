package fpt.capstone.exceprion.handler;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.ArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

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
}
