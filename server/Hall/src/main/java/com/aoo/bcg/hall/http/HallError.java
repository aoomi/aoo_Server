package com.aoo.bcg.hall.http;

public final class HallError extends RuntimeException {
    private final int status;
    private final String code;
    public HallError(int status,String code,String message){super(message);this.status=status;this.code=code;}
    public int status(){return status;}
    public String code(){return code;}
    public static HallError bad(String code,String message){return new HallError(400,code,message);}
    public static HallError conflict(String code,String message){return new HallError(409,code,message);}
    public static HallError notFound(String message){return new HallError(404,"HALL_NOT_FOUND",message);}
}
