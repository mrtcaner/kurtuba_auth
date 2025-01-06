package com.kurtuba.auth.error.handlers;

import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.error.exception.ResourceNotFoundException;
import com.kurtuba.auth.data.dto.ResponseErrorDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<?> businessLogicException(BusinessLogicException ex, WebRequest request) {
        ex.printStackTrace();
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ex.printStackTrace();
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.RESOURCE_NOT_FOUND.getCode())
                .message(ErrorEnum.RESOURCE_NOT_FOUND.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<?> constraintViolationException(ConstraintViolationException ex, WebRequest request) {
        ex.printStackTrace();
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getConstraintViolations().toString())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HandlerMethodValidationException.class})
    public ResponseEntity<?> handlerMethodValidationException(HandlerMethodValidationException ex, WebRequest request) {
        ex.printStackTrace();
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getAllValidationResults().toString())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<?> methodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        ex.printStackTrace();
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.INVALID_PARAMETER.getCode())
                .message(ex.getMessage())
                .error(ex.getBody().getDetail())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
}