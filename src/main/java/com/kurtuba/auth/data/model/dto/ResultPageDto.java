package com.kurtuba.auth.data.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ResultPageDto {

    private boolean success;

    private static final String bgColorSuccess = "#65c18c";

    private static final String iconSuccess = "fa-check";

    private static final String bgColorFailure = "#ba0f0f";

    private static final String iconFailure = "fa-exclamation";

    private String title;

    private String message1;

    private String message2;

    public Map<String,String> toMap(){
        Map dtoMap = new HashMap<>();
        dtoMap.put("bgColor", success ? bgColorSuccess : bgColorFailure);
        dtoMap.put("icon", success ? iconSuccess : iconFailure);
        dtoMap.put("title", title);
        dtoMap.put("message1", message1);
        dtoMap.put("message2", message2);
        return dtoMap;
    }

}
