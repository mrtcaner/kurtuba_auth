package com.kurtuba.auth.error.handlers;


import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.utils.response.ResponseError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

//default lowest precedence, so works if no specific handlers found
@ControllerAdvice
public class BaseExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        ex.printStackTrace();
        ResponseError errorDetails = ResponseError
                .builder()
                .code(ErrorEnum.GENERIC_EXCEPTION.getCode())
                .message(ErrorEnum.GENERIC_EXCEPTION.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        //In case of RFC 9457 error responses such as 404, 405 etc
        if(ex instanceof ErrorResponse){
            return new ResponseEntity<>(errorDetails, HttpStatus.valueOf(((ErrorResponse) ex).getBody().getStatus()));
        }

        // For anything else such as RuntimeException etc
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);

    }
}