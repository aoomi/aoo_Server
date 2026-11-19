package com.aoo.bcg.hall.http;

import com.aoo.bcg.common.error.ErrorCodeCatalog;

public final class HallError extends RuntimeException {
    private static final ErrorCodeCatalog ERROR_CODES = ErrorCodeCatalog.standard();
    private final int status;
    private final String code;
    public HallError(int status,String code,String message){super(message);this.status=status;this.code=code;}
    public int status(){return status;}
    public String code(){return code;}
    /** Canonical cross-service category while preserving the existing wire code. */
    public String canonicalCode(){
        try{return ERROR_CODES.canonicalSymbolicCode(code);}
        catch(IllegalArgumentException ignored){return code;}
    }
    public static HallError bad(String code,String message){return new HallError(400,code,message);}
    public static HallError conflict(String code,String message){return new HallError(409,code,message);}
    public static HallError notFound(String message){return new HallError(404,"HALL_NOT_FOUND",message);}
}
