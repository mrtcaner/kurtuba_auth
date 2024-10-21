package com.parafusion.auth.utils.response;

public enum ResponseType {

    ERROR("error"),
    DATA("data");

    private String type;

    ResponseType(String type){
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
