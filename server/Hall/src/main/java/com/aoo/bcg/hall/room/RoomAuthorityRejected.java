package com.aoo.bcg.hall.room;

final class RoomAuthorityRejected extends RuntimeException {
    private final int status;
    private final String code;
    RoomAuthorityRejected(int status,String code,String message){super(message);this.status=status;this.code=code;}
    int status(){return status;}
    String code(){return code;}
}
