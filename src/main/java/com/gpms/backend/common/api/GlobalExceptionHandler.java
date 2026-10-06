package com.gpms.backend.common.api;

import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.common.exception.ConflictException;
import com.gpms.backend.common.exception.ResourceNotFoundException;
import com.gpms.backend.common.exception.UnauthorizedOperationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({BusinessException.class, UnauthorizedOperationException.class})
    public ResponseEntity<ApiError> handleBusiness(RuntimeException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (left, right) -> left));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), errors);
    }

    /*
     * A wrong password is not a server fault.
     *
     * Spring throws BadCredentialsException out of the authentication
     * manager, and nothing here caught it - so a mistyped password fell
     * through to the handler below and came back as a 500. The app read
     * that as "the server is having trouble, try later", which sent
     * people away to wait for a problem that was never going to clear.
     *
     * DisabledException is called out separately because "your account
     * is switched off" and "you typed the wrong password" are different
     * problems and only one of them is worth retrying.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(
            DisabledException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED,
                "This account has been disabled. Ask your admin to re-enable it.",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        /*
         * Deliberately the same answer whether the username does not
         * exist or the password was wrong - telling them apart is how an
         * attacker enumerates accounts.
         */
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnhandled(Exception exception, HttpServletRequest request) {
        /*
         * The real message is logged, never returned. Hibernate and
         * Postgres put constraint names, column names and SQL fragments
         * in it, which is a free map of the schema for anyone poking at
         * the API.
         */
        LOG.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request.getRequestURI(), Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
        return ResponseEntity.status(status).body(error);
    }
}
