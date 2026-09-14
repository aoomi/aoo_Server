package com.aoo.bcg.support;
import java.time.Instant;
import java.util.List;
public record SupportCase(long id,long playerId,Kind kind,String subject,String description,Status status,String resolutionSummary,List<Evidence> evidence,Instant createdAt,Instant updatedAt,long version){
 public enum Kind{REPORT,APPEAL,TICKET} public enum Status{OPEN,IN_REVIEW,RESOLVED,REJECTED,WITHDRAWN}
 public record Evidence(Type type,String referenceId,String label){public enum Type{REPLAY,ADMIN_CASE,URL}}
}
