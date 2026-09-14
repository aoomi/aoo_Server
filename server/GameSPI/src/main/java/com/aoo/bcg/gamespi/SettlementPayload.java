package com.aoo.bcg.gamespi;
import java.util.Map;
public record SettlementPayload(long roomId,int roundNo,String playVersion,Map<Long,Long> scoreDelta){
 public SettlementPayload{if(roomId<=0||roundNo<=0||playVersion==null||playVersion.isBlank()||scoreDelta==null||scoreDelta.isEmpty())throw new IllegalArgumentException("invalid settlement payload");scoreDelta=Map.copyOf(scoreDelta);}
}
