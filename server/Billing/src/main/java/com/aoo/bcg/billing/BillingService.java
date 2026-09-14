package com.aoo.bcg.billing;
public interface BillingService {
    LedgerEntry debit(String businessId, long playerId, String currency, long amount, String reasonCode);
    LedgerEntry credit(String businessId, long playerId, String currency, long amount, String reasonCode);
    default LedgerEntry debit(String businessId,CurrencyAccount account,long amount,String reasonCode){
        if(account.scopeId()!=0)throw new UnsupportedOperationException("scoped currency requires authoritative scoped implementation");
        return debit(businessId,account.playerId(),account.currency(),amount,reasonCode);
    }
    default LedgerEntry credit(String businessId,CurrencyAccount account,long amount,String reasonCode){
        if(account.scopeId()!=0)throw new UnsupportedOperationException("scoped currency requires authoritative scoped implementation");
        return credit(businessId,account.playerId(),account.currency(),amount,reasonCode);
    }
}
