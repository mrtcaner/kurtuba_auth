package com.kurtuba.auth.data.enums;

public enum AuthoritiesType {
    USER(0,"User"),
    ADMIN(1,"Admin"),
    SERVICE(2,"Service");

    AuthoritiesType(int id, String displayName){
        this.id = id;
        this.displayName = displayName;
    }

    final int id;
    final String displayName;

    public int getId(){
        return id;
    }

    public String getDisplayName(){
        return displayName;
    }

    AuthoritiesType fromDisplayName(String name){
        if(AuthoritiesType.USER.displayName.equals(name.toUpperCase()))
            return AuthoritiesType.USER;
        if(AuthoritiesType.ADMIN.displayName.equals(name.toUpperCase()))
            return AuthoritiesType.ADMIN;
        if(AuthoritiesType.SERVICE.displayName.equals(name.toUpperCase()))
            return AuthoritiesType.SERVICE;
        return null;
    }
}