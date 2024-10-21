package com.parafusion.auth.data.model;

public enum ClientType {

    MOBILE_CLIENT("mobile-client"),
    WEB_CLIENT("web-client");

    private String clientTypeName;

    ClientType(String clientTypeName){
        this.clientTypeName = clientTypeName;
    }

    public String getClientTypeName(){
        return clientTypeName;
    }

    public static ClientType fromName(String name){
        if(ClientType.MOBILE_CLIENT.name() == name.toUpperCase())
            return ClientType.MOBILE_CLIENT;
        if(ClientType.WEB_CLIENT.name() == name.toUpperCase())
            return ClientType.WEB_CLIENT;
        return null;
    }
}
