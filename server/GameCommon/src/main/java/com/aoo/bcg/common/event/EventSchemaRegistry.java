package com.aoo.bcg.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Explicit adjacent-version upcasters; unknown future schemas fail closed. */
public final class EventSchemaRegistry{
    private final ConcurrentHashMap<String,Integer> current=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key,UnaryOperator<JsonNode>> upcasters=new ConcurrentHashMap<>();
    public EventSchemaRegistry register(String eventType,int currentVersion){if(eventType==null||eventType.isBlank()||currentVersion<=0)throw new IllegalArgumentException("invalid event schema");current.put(eventType,currentVersion);return this;}
    public EventSchemaRegistry upcaster(String eventType,int fromVersion,UnaryOperator<JsonNode> upcaster){if(fromVersion<=0||upcaster==null)throw new IllegalArgumentException("invalid upcaster");upcasters.put(new Key(eventType,fromVersion),upcaster);return this;}
    public JsonNode upcast(String eventType,int sourceVersion,JsonNode payload){int target=current.getOrDefault(eventType,1);if(sourceVersion>target)throw new IllegalArgumentException("unsupported future event schema");JsonNode value=payload;for(int version=sourceVersion;version<target;version++){UnaryOperator<JsonNode> converter=upcasters.get(new Key(eventType,version));if(converter==null)throw new IllegalStateException("missing adjacent event upcaster");value=converter.apply(value);}return value;}
    public void requireSupported(String eventType,int version){upcast(eventType,version,com.fasterxml.jackson.databind.node.NullNode.instance);}
    private record Key(String eventType,int fromVersion){}
}
