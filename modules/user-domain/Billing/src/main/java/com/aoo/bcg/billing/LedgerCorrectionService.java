package com.aoo.bcg.billing;
import java.util.Objects;
/** Corrects money only by appending an opposite immutable ledger entry. */
public final class LedgerCorrectionService{
 private final BillingService billing;private final LedgerRepository ledger;
 public LedgerCorrectionService(BillingService billing,LedgerRepository ledger){this.billing=Objects.requireNonNull(billing);this.ledger=Objects.requireNonNull(ledger);}
 public LedgerEntry reverse(String correctionBusinessId,String originalBusinessId,String reasonCode){if(correctionBusinessId==null||correctionBusinessId.isBlank()||originalBusinessId==null||originalBusinessId.isBlank()||correctionBusinessId.equals(originalBusinessId)||reasonCode==null||!reasonCode.matches("[A-Z0-9_.-]{1,32}"))throw new IllegalArgumentException("distinct business ids and valid reason required");LedgerEntry original=ledger.findByBusinessId(originalBusinessId).orElseThrow(()->new IllegalArgumentException("original ledger entry not found"));String reason=reason(reasonCode,originalBusinessId);if(original.delta()>0)return billing.debit(correctionBusinessId,original.playerId(),original.currency(),original.delta(),reason);if(original.delta()<0)return billing.credit(correctionBusinessId,original.playerId(),original.currency(),Math.negateExact(original.delta()),reason);throw new IllegalArgumentException("zero ledger entry cannot be reversed");}
 private static String reason(String code,String original){String clear="CORRECTION:"+code+":"+original;if(clear.length()<=64)return clear;return "CORRECTION:"+code+":"+PaymentProductSnapshot.digest(original).substring(0,16);}
}
