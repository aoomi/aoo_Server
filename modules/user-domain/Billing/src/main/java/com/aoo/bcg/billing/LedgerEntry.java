package com.aoo.bcg.billing;
import java.time.Instant;
public record LedgerEntry(String businessId, long playerId, String currency, long delta, long balanceAfter,
                          String reasonCode,String entryStatus,String channelCode,Instant createdAt) {
 public LedgerEntry(String businessId,long playerId,String currency,long delta,long balanceAfter,String reasonCode,Instant createdAt){this(businessId,playerId,currency,delta,balanceAfter,reasonCode,"POSTED",channel(reasonCode),createdAt);}
 public LedgerEntry{
  if(businessId==null||businessId.isBlank()||playerId<=0||currency==null||reasonCode==null||createdAt==null||delta==0)throw new IllegalArgumentException("invalid ledger entry");
  if(entryStatus==null||!entryStatus.matches("POSTED|REVERSED|PENDING")||channelCode==null||!channelCode.matches("[A-Z0-9_]{1,32}"))throw new IllegalArgumentException("invalid ledger reconciliation dimensions");
  Math.subtractExact(balanceAfter,delta);
 }
 /** The immutable before-balance is derivable without trusting a mutable balance row. */
 public long balanceBefore(){return Math.subtractExact(balanceAfter,delta);}
 private static String channel(String reason){if(reason==null||reason.isBlank())return"GAME";String value=reason.split("[_.-]",2)[0].toUpperCase(java.util.Locale.ROOT);return value.matches("[A-Z0-9_]{1,32}")?value:"GAME";}
}
