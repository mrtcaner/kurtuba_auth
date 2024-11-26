package com.kurtuba.auth.data.model;

import lombok.Data;

public enum RoleEnum{
    USER(0,"User"),
    ADMIN(1,"Admin");

    RoleEnum(int id, String displayName){
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

    RoleEnum fromDisplayName(String name){
        if(RoleEnum.USER.displayName.equals(name.toUpperCase()))
            return RoleEnum.USER;
        if(RoleEnum.ADMIN.displayName.equals(name.toUpperCase()))
            return RoleEnum.ADMIN;
        return null;
    }
}