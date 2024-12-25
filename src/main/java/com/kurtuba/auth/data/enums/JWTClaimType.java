package com.kurtuba.auth.data.enums;

public enum JWTClaimType {
    KID(0,"kid"),
    SUB(1,"sub"),
    AUD(2,"aud"),
    NBF(3,"nbf"),
    ISS(4,"iss"),
    EXP(5,"exp"),
    IAT(6,"iat"),
    JTI(7,"jti"),
    SCOPE(8,"scope");

    JWTClaimType(int id, String displayName){
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
}
