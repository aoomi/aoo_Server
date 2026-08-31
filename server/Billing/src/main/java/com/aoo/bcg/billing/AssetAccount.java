package com.aoo.bcg.billing;

import java.util.Objects;

/** Canonical address for currencies, points and item quantities across personal and scoped ledgers. */
public record AssetAccount(Principal principal,long principalId,Kind kind,String assetCode,Scope scope,long scopeId){
    public AssetAccount{
        Objects.requireNonNull(principal,"principal");Objects.requireNonNull(kind,"kind");Objects.requireNonNull(scope,"scope");
        if(principalId<=0||assetCode==null||!assetCode.matches("[A-Z0-9_.-]{1,64}"))throw new IllegalArgumentException("invalid asset account");
        if((scope==Scope.GLOBAL)!=(scopeId==0))throw new IllegalArgumentException("invalid asset scope");
        if(kind==Kind.CURRENCY&&!assetCode.matches("ROOM_CARD|CITY_ROOM_CARD|GOLD|CRYSTAL|SPORTS_POINT"))throw new IllegalArgumentException("unsupported currency");
        if(assetCode.equals("CITY_ROOM_CARD")&&scope!=Scope.CITY)throw new IllegalArgumentException("city room card requires city scope");
        if(assetCode.equals("SPORTS_POINT")&&scope!=Scope.UNION)throw new IllegalArgumentException("sports point requires union scope");
        if(kind==Kind.CURRENCY&&!assetCode.equals("CITY_ROOM_CARD")&&!assetCode.equals("SPORTS_POINT")&&scope!=Scope.GLOBAL)
            throw new IllegalArgumentException("global currency cannot be scoped");
    }
    public static AssetAccount currency(CurrencyAccount account){
        Scope scope=account.scopeId()==0?Scope.GLOBAL:account.currency().equals("CITY_ROOM_CARD")?Scope.CITY:Scope.UNION;
        return new AssetAccount(Principal.PLAYER,account.playerId(),Kind.CURRENCY,account.currency(),scope,account.scopeId());
    }
    public static AssetAccount item(long playerId,String itemCode,Scope scope,long scopeId){return new AssetAccount(Principal.PLAYER,playerId,Kind.ITEM,itemCode,scope,scopeId);}
    public String canonicalId(){return principal+"/"+principalId+"/"+kind+"/"+assetCode+"/"+scope+"/"+scopeId;}
    public CurrencyAccount toCurrencyAccount(){if(principal!=Principal.PLAYER||kind!=Kind.CURRENCY)throw new IllegalStateException("not a player currency account");return new CurrencyAccount(principalId,assetCode,scopeId);}
    public enum Principal{PLAYER,CLUB,UNION,AGENT,SYSTEM}
    public enum Kind{CURRENCY,ITEM}
    public enum Scope{GLOBAL,CITY,CLUB,UNION}
}
