package com.kurtuba.auth.error.handlers;

import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicError;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.error.exception.ResourceNotFoundException;
import com.kurtuba.auth.data.dto.ResponseErrorDto;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ResponseErrorDto> businessLogicException(BusinessLogicException ex, WebRequest request) {
        LOGGER.warn("Business logic exception for request {} with code {}", request.getDescription(false), ex.getErrorCode(), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseErrorDto> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        LOGGER.warn("Resource not found for request {}", request.getDescription(false), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.RESOURCE_NOT_FOUND.getCode())
                .message(ErrorEnum.RESOURCE_NOT_FOUND.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ResponseErrorDto> constraintViolationException(ConstraintViolationException ex, WebRequest request) {
        LOGGER.warn("Constraint violation for request {}", request.getDescription(false), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getConstraintViolations().toString())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HandlerMethodValidationException.class})
    public ResponseEntity<ResponseErrorDto> handlerMethodValidationException(HandlerMethodValidationException ex, WebRequest request) {
        LOGGER.warn("Handler method validation failed for request {}", request.getDescription(false), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getParameterValidationResults().toString())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ResponseErrorDto> methodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        LOGGER.warn("Method argument validation failed for request {}", request.getDescription(false), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getMessage())
                .error(ex.getBody().getDetail())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessLogicError.class)
    public ResponseEntity<ResponseErrorDto> businessLogicError(BusinessLogicError ex, WebRequest request) {
        LOGGER.error("Business logic error for request {} with code {}", request.getDescription(false), ex.getErrorCode(), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
