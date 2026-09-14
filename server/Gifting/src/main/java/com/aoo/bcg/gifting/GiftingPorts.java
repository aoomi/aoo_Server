package com.aoo.bcg.gifting;

public final class GiftingPorts {
 private GiftingPorts(){}
 public interface AssetAuthority { void debit(String key,long player,String asset,long quantity)throws Exception; void credit(String key,long player,String asset,long quantity)throws Exception; }
 public interface RiskAuthority { Decision evaluate(long sender,long recipient,String asset,long quantity,String device,String ip)throws Exception; }
 public record Decision(boolean allowed,String code,int score){public Decision{if(code==null||code.isBlank())throw new IllegalArgumentException("risk code required");}}
}
