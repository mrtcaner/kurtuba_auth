package com.parafusion.auth.error.handlers;


import com.parafusion.auth.error.enums.ErrorEnum;
import com.parafusion.auth.utils.response.ResponseError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

//default lowest precedence, so works if no specific handlers found
@ControllerAdvice
public class BaseExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExcpetionHandler(Exception ex, WebRequest request) {
        ex.printStackTrace();
        ResponseError errorDetails = ResponseError
                .builder()
                .code(ErrorEnum.GENERIC_EXCEPTION.getCode())
                .message(ErrorEnum.GENERIC_EXCEPTION.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}