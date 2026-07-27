package com.nimbus.backend.common.exception;

import com.nimbus.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(AlreadyExistsException ex){
        ApiResponse<Void> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        // Extract the first validation error message
        String errorMsg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new ResponseEntity<>(new ApiResponse<>(false, errorMsg, null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return new ResponseEntity<>(new ApiResponse<>(false, "An unexpected error occurred", null), HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
