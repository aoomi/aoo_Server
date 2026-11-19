package com.aoo.bcg.referral;

import java.math.BigDecimal;
import java.util.Map;

public final class PromoterService {
    private final JdbcPromoterRepository repository;private final BillingAuthorityPort billing;
    public PromoterService(JdbcPromoterRepository repository,BillingAuthorityPort billing){this.repository=repository;this.billing=billing;}
    public Map<String,Object> bind(long club,long player,long promoter){return repository.bind(club,player,promoter);}
    public Map<String,Object> relation(long club,long player){return repository.relation(club,player);}
    public Map<String,Object> summary(long club,long promoter,int from,int to){return repository.summary(club,promoter,from,to);}
    public Map<String,Object> calculate(String id,long club,long promoter,int from,int to,BigDecimal rate,String currency){return repository.calculate(id,club,promoter,from,to,rate,currency);}
    public Map<String,Object> status(String id){return repository.status(id);}
    public Map<String,Object> settle(String id)throws Exception{Map<String,Object> state=repository.status(id);if("SETTLED".equals(state.get("status")))return state;if(!"CALCULATED".equals(state.get("status")))throw new IllegalStateException("commission is not settleable");billing.credit("promoter-commission:"+id,(long)state.get("promoterId"),(long)state.get("clubId"),(String)state.get("currency"),(BigDecimal)state.get("commissionAmount"),"PROMOTER_COMMISSION");return repository.markSettled(id);}
    public Map<String,Object> inviteReward(String key,long consumption,long invite,long issuer,long consumer,String kind,long target)throws Exception{Map<String,Object>s=repository.reserveInviteReward(key,consumption,invite,issuer,consumer,kind,target);if("GRANTED".equals(s.get("status")))return s;billing.credit("invite-reward:"+consumption,consumer,target,(String)s.get("currency"),(BigDecimal)s.get("amount"),"INVITE_CONSUMED");return repository.completeInviteReward(key);}
}
