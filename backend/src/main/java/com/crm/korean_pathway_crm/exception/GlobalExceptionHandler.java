package com.crm.korean_pathway_crm.exception;

import com.crm.korean_pathway_crm.common.exception.ResourceNotFoundException;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
                                                              HttpHeaders headers, HttpStatusCode status,
                                                              WebRequest request) {
        ErrorCode error = frameworkError(exception, status);
        String message = error.getMessage();
        if (exception instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .sorted(Comparator.comparing(field -> field.getField()))
                    .map(field -> field.getDefaultMessage())
                    .filter(Objects::nonNull)
                    .findFirst().orElse(message);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(exception, responseBody(error, message), responseHeaders, status, request);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        return errorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException exception) {
        return errorResponse(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidArgument(Exception exception) {
        return errorResponse(ErrorCode.INVALID_PARAMETER);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return errorResponse(ErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        return errorResponse(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict(DataIntegrityViolationException exception) {
        return errorResponse(ErrorCode.DATA_CONFLICT);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConcurrentModification(Exception exception) {
        return errorResponse(ErrorCode.CONCURRENT_MODIFICATION);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseUnavailable(Exception exception) {
        log.warn("Database unavailable: {}", exception.getClass().getSimpleName());
        return errorResponse(ErrorCode.DATABASE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled application exception: {}", exception.getClass().getSimpleName());
        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ErrorCode frameworkError(Exception exception, HttpStatusCode status) {
        if (exception instanceof TypeMismatchException) {
            return ErrorCode.INVALID_PARAMETER;
        }
        if (exception instanceof MissingServletRequestParameterException
                || exception instanceof MissingServletRequestPartException) {
            return ErrorCode.MISSING_PARAMETER;
        }
        return switch (status.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 401 -> ErrorCode.INVALID_ACCESS_TOKEN;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.RESPONSE_TYPE_NOT_ACCEPTABLE;
            case 409 -> ErrorCode.DATA_CONFLICT;
            case 413 -> ErrorCode.FILE_TOO_LARGE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 503 -> ErrorCode.DATABASE_UNAVAILABLE;
            default -> ErrorCode.UNCATEGORIZED_EXCEPTION;
        };
    }

    private ResponseEntity<ApiResponse<Void>> errorResponse(ErrorCode error) {
        return ResponseEntity.status(error.getHttpStatus()).contentType(MediaType.APPLICATION_JSON)
                .body(responseBody(error, error.getMessage()));
    }

    private ApiResponse<Void> responseBody(ErrorCode error, String message) {
        return ApiResponse.<Void>builder().code(error.getCode()).message(message).build();
    }
}
