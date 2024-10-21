package com.parafusion.auth.utils.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseError{

    private final String type = ResponseType.ERROR.getType();
    private LocalDateTime timestamp;
    private String error;
    private Integer code;
    private String message;
    private String detail;
}
