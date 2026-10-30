package com.aoo.bcg.admin;import java.time.Instant;
@FunctionalInterface public interface AdminAuthorizationAudit{void record(String requestId,long operatorId,String permission,String method,String path,boolean allowed,Instant occurredAt);}
