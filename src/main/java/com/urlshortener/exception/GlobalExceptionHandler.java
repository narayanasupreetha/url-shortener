package com.urlshortener.exception;

import com.urlshortener.dto.ApiError;
import com.urlshortener.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle Email Already Exists
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        return buildError(
                "USER_ALREADY_EXISTS",
                "Email already registered",
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    // Handle Validation Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex) {

        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildError(
                "VALIDATION_ERROR",
                "Invalid request payload",
                details,
                HttpStatus.BAD_REQUEST
        );
    }

    // Handle No Resource Found Exception
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        return buildError(
                "NOT_FOUND",
                "Resource not found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // Handle Generic Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {

        return buildError(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ApiError> buildError(
            String code,
            String message,
            String details,
            HttpStatus status) {

        String correlationId = Optional
                .ofNullable(MDC.get(CorrelationIdFilter.CORRELATION_ID))
                .orElse("N/A");

        ApiError error = new ApiError(
                code,
                message,
                details,
                LocalDateTime.now(),
                correlationId
        );

        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return buildError(
                "UNAUTHORIZED",
                "Authentication failed",
                "Invalid email or password",
                HttpStatus.UNAUTHORIZED
        );
    }

    // Handle Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        return buildError(
                "UNAUTHORIZED",
                "Authentication failed",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    // Handle Resource Not Found Exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(
                "NOT FOUND",
                "Resource not found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    //Handle Username Not Found Exception
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {

        return buildError(
                "NOT FOUND",
                "Resource not found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // Handle Link Gone Exception
    @ExceptionHandler(LinkGoneException.class)
    public ResponseEntity<?> handleLinkGone(LinkGoneException ex) {

        return buildError(
                "GONE",
                "The short link is no longer available",
                ex.getMessage(),
                HttpStatus.GONE
        );
    }

    // Handle Response Status Exception
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex
    ) {

        HttpStatus status = (HttpStatus) ex.getStatusCode();

        return buildError(
                status.name(),
                ex.getReason(),
                null,
                status
        );
    }
}