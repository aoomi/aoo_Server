package com.aoo.bcg.common.idempotency;

import java.time.Instant;

public record IdempotencyResult<R>(int code,String resultVersion,int schemaVersion,R data,Instant expiresAt){
    public IdempotencyResult{if(resultVersion==null||resultVersion.isBlank()||schemaVersion<=0||data==null||expiresAt==null)throw new IllegalArgumentException("invalid idempotency result");}
    public static <R> IdempotencyResult<R> success(R data,Instant expiresAt){return new IdempotencyResult<>(0,"v1",1,data,expiresAt);}
}
