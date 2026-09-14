package com.aoo.bcg.inventory;

import java.time.Instant;import java.util.*;

public interface InventoryStoreService {
 record Item(String code,String name,String type,boolean consumable,Instant activeFrom,Instant activeUntil,Map<String,Object> attributes){}
 record Stock(String itemCode,long quantity,Instant expiresAt,long version){}
 record Offer(String code,String itemCode,long quantity,String currency,long price,Instant activeFrom,Instant activeUntil){}
 record Command(String commandId,String kind,String state,long playerId,String referenceCode,Instant createdAt){}
 List<Item> catalog(Instant at);List<Stock> inventory(long playerId,Instant at);List<Offer> offers(Instant at);
 Command grant(String idempotencyKey,long playerId,String itemCode,long quantity,Instant expiresAt,String source);
 Command purchase(String idempotencyKey,long playerId,String offerCode);
 Command use(String idempotencyKey,long playerId,String itemCode,long quantity);
 Command redeem(String idempotencyKey,long playerId,String code);
 Command revoke(String idempotencyKey,String originalCommandId,String reason);
 int expire(Instant at,int limit);
}
