package com.aoo.bcg.lobby;

public class LobbyRoomAuthorityRejected extends RuntimeException {
    private final int status;
    private final String code;
    public LobbyRoomAuthorityRejected(int status,String code,String message){this(status,code,message,null);}
    public LobbyRoomAuthorityRejected(int status,String code,String message,Throwable cause){super(message,cause);this.status=status;this.code=code;}
    public int status(){return status;}
    public String code(){return code;}
}
