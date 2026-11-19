package com.aoo.bcg.share;

public final class InviteLinkError extends RuntimeException {
    private final int status; private final String code;
    public InviteLinkError(int status,String code,String message){super(message);this.status=status;this.code=code;}
    public int status(){return status;} public String code(){return code;}
}
