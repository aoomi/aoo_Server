package com.aoo.bcg.common.concurrency;
import java.time.Instant;import java.util.*;
/** Structured, redacted deadlock evidence suitable for logs/metrics and idempotent retry correlation. */
public record DatabaseDeadlockDiagnostic(String idempotencyKey,List<String>statements,List<String>lockObjects,List<String>businessKeys,String sqlState,int vendorCode,Instant observedAt){
 public DatabaseDeadlockDiagnostic{if(idempotencyKey==null||idempotencyKey.isBlank())throw new IllegalArgumentException("idempotency key required");statements=copy(statements,"statements");lockObjects=copy(lockObjects,"lock objects");businessKeys=copy(businessKeys,"business keys");Objects.requireNonNull(observedAt);}
 private static List<String>copy(List<String>v,String name){Objects.requireNonNull(v,name);if(v.isEmpty())throw new IllegalArgumentException(name+" required");return List.copyOf(v);}
 public static DatabaseDeadlockDiagnostic capture(String idempotencyKey,List<String>statements,List<String>locks,List<String>keys,Throwable failure){java.sql.SQLException sql=null;for(Throwable c=failure;c!=null;c=c.getCause())if(c instanceof java.sql.SQLException x){sql=x;break;}return new DatabaseDeadlockDiagnostic(idempotencyKey,statements,locks,keys,sql==null?null:sql.getSQLState(),sql==null?0:sql.getErrorCode(),Instant.now());}
}
