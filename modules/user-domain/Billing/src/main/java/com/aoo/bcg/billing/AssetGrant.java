package com.aoo.bcg.billing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Persistable reward/gift/agent-allocation aggregate with explicit source, target, tax and risk result. */
public record AssetGrant(String businessId,Kind kind,CurrencyAccount source,CurrencyAccount target,
        CurrencyAccount taxAccount,long grossAmount,long taxAmount,long netAmount,String sourceCode,
        LocalDate businessDate,RiskDecision riskDecision,State state,String failureCode,long version,
        Instant createdAt,Instant completedAt){
    public AssetGrant{
        if(businessId==null||!businessId.matches("[A-Za-z0-9_.:-]{1,128}")||kind==null||target==null
                ||grossAmount<=0||taxAmount<0||netAmount<=0||Math.addExact(taxAmount,netAmount)!=grossAmount
                ||sourceCode==null||!sourceCode.matches("[A-Z0-9_.-]{1,64}")||businessDate==null
                ||riskDecision==null||state==null||version<=0||createdAt==null)throw new IllegalArgumentException("invalid asset grant");
        if(kind.requiresSource()&&source==null)throw new IllegalArgumentException("grant source required");
        if(!kind.requiresSource()&&source!=null)throw new IllegalArgumentException("grant source must be absent");
        if(source!=null&&(source.equals(target)||!source.currency().equals(target.currency())))throw new IllegalArgumentException("invalid transfer accounts");
        if(taxAmount>0&&(taxAccount==null||!taxAccount.currency().equals(target.currency())||taxAccount.equals(target)))
            throw new IllegalArgumentException("tax account required");
        if(taxAmount==0&&taxAccount!=null)throw new IllegalArgumentException("unexpected tax account");
        if((state==State.COMPLETED||state==State.COMPENSATED)&&completedAt==null)throw new IllegalArgumentException("terminal grant lacks timestamp");
    }
    public enum Kind{
        REWARD(false),COMPENSATION(false),GIFT(true),AGENT_ALLOCATION(true);
        private final boolean requiresSource;Kind(boolean requiresSource){this.requiresSource=requiresSource;}public boolean requiresSource(){return requiresSource;}
    }
    public enum RiskDecision { ALLOW, REVIEW, REJECT }
    public enum State { CREATED, REVIEW, REJECTED, COMPLETED, COMPENSATION_PENDING, COMPENSATED }
}
