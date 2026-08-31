package com.aoo.bcg.gamespi.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Append-only WSS message-id ledger; retired identifiers are permanent tombstones. */
public final class MessageIdLedger {
    public enum State { ACTIVE, RETIRED }
    public record Entry(String messageId,String ownerModule,SemanticVersion introducedVersion,State state,SemanticVersion retiredVersion){
        public Entry{
            if(messageId==null||!messageId.matches("[a-z][a-z0-9_-]*(?:\\.[a-z][a-z0-9_-]*)+_(?:req|resp|push)")
                    ||ownerModule==null||ownerModule.isBlank()||introducedVersion==null||state==null
                    ||(state==State.RETIRED&&retiredVersion==null)||(state==State.ACTIVE&&retiredVersion!=null))throw new IllegalArgumentException("invalid message-id ledger entry");
        }
    }
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    public synchronized void register(String messageId,String owner,SemanticVersion introduced){
        Entry candidate=new Entry(messageId,owner,introduced,State.ACTIVE,null);Entry existing=entries.get(messageId);
        if(existing!=null&&!existing.equals(candidate))throw new IllegalStateException("message id is already published or permanently retired: "+messageId);
        entries.putIfAbsent(messageId,candidate);
    }
    public synchronized void retire(String messageId,SemanticVersion version){
        Entry existing=require(messageId);if(existing.state()==State.RETIRED)throw new IllegalStateException("message id already retired");
        entries.put(messageId,new Entry(messageId,existing.ownerModule(),existing.introducedVersion(),State.RETIRED,version));
    }
    public synchronized Entry require(String messageId){Entry value=entries.get(messageId);if(value==null)throw new IllegalArgumentException("unpublished message id: "+messageId);return value;}
    public synchronized Map<String,Entry> snapshot(){return Map.copyOf(entries);}
}
