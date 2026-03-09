package com.expmatik.backend.exceptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRoleException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> invalidRoleException(InvalidRoleException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value(), new Date());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> resourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidPasswordException(InvalidPasswordException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(UserExistsException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> userExistsException(UserExistsException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidRefreshTokenException(InvalidRefreshTokenException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(InvalidHashException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidHashException(InvalidHashException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> conflictException(ConflictException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), new Date());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }
    
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> badRequestException(BadRequestException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(FileSizeExceededException.class)
    @ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<ErrorResponse> fileSizeExceededException(FileSizeExceededException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE.value(), new Date());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        errorResponse.put("message", "Validation failed");
        errorResponse.put("statusCode", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("errors", fieldErrors);
        errorResponse.put("date", new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> constraintViolationException(ConstraintViolationException ex) {
        String messageText = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation failed");

        ErrorResponse message = new ErrorResponse(messageText, HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorResponse message = new ErrorResponse("Invalid request body: " + ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<ErrorResponse> max2mbUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        ErrorResponse message = new ErrorResponse("File size exceeds maximum allowed size (2MB)", HttpStatus.PAYLOAD_TOO_LARGE.value(), new Date());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> accessDeniedException(AccessDeniedException ex) {
        ErrorResponse message = new ErrorResponse("Access denied: " + ex.getMessage(), HttpStatus.FORBIDDEN.value(), new Date());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> unauthorizedActionException(UnauthorizedActionException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value(), new Date());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> globalExceptionHandler(Exception ex) {
        ErrorResponse message = new ErrorResponse("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), new Date());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }

}



