package com.kurtuba.auth.error.handlers;


import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.data.dto.ResponseErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

//default lowest precedence, so works if no specific handlers found
@ControllerAdvice
public class BaseExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDto> globalExceptionHandler(Exception ex, WebRequest request) {
        LOGGER.error("Unhandled exception for request {}", request.getDescription(false), ex);
        ResponseErrorDto errorDetails = ResponseErrorDto
                .builder()
                .code(ErrorEnum.GENERIC_EXCEPTION.getCode())
                .message(ErrorEnum.GENERIC_EXCEPTION.getMessage())
                .error(ex.getMessage())
                .detail(request.getDescription(false))
                .timestamp(Instant.now())
                .build();

        //In case of RFC 9457 error responses such as 404, 405 etc
        if(ex instanceof ErrorResponse){
            return new ResponseEntity<>(errorDetails, HttpStatus.valueOf(((ErrorResponse) ex).getBody().getStatus()));
        }

        //In case of Access Denied
        if(ex instanceof AuthorizationDeniedException){
            return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
        }

        // For anything else such as RuntimeException etc
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
