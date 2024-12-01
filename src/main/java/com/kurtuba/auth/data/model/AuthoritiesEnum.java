package com.kurtuba.auth.data.model;

public enum AuthoritiesEnum {
    USER(0,"User"),
    ADMIN(1,"Admin"),
    SERVICE(2,"Service");

    AuthoritiesEnum(int id, String displayName){
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

    AuthoritiesEnum fromDisplayName(String name){
        if(AuthoritiesEnum.USER.displayName.equals(name.toUpperCase()))
            return AuthoritiesEnum.USER;
        if(AuthoritiesEnum.ADMIN.displayName.equals(name.toUpperCase()))
            return AuthoritiesEnum.ADMIN;
        if(AuthoritiesEnum.SERVICE.displayName.equals(name.toUpperCase()))
            return AuthoritiesEnum.SERVICE;
        return null;
    }
}