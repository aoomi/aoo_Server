package com.aoo.bcg.gamespi.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Governs staged field retirement without silently breaking older clients. */
public final class FieldDeprecationRegistry {
    public record FieldDeprecation(ApiOwnershipCatalog.Transport transport, String endpoint, String field,
                                   SemanticVersion deprecatedSince, SemanticVersion stopProducingAt,
                                   SemanticVersion stopConsumingAt, int removeInMajor, String replacement) {
        public FieldDeprecation {
            if (transport==null||endpoint==null||endpoint.isBlank()||field==null||field.isBlank()
                    ||deprecatedSince==null||stopProducingAt==null||stopConsumingAt==null||removeInMajor<=deprecatedSince.major()
                    ||replacement==null||replacement.isBlank()) throw new IllegalArgumentException("incomplete field deprecation");
            if(stopProducingAt.compareTo(deprecatedSince)<0||stopConsumingAt.compareTo(stopProducingAt)<0)
                throw new IllegalArgumentException("invalid field deprecation sequence");
        }
    }
    public record Usage(long produced,long consumed) {}
    private record Entry(FieldDeprecation metadata,LongSupplier produced,LongSupplier consumed) {}
    private final ApiLifecycleCatalog lifecycle;
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    public FieldDeprecationRegistry(ApiLifecycleCatalog lifecycle){this.lifecycle=lifecycle;}
    public synchronized void register(FieldDeprecation value,LongSupplier produced,LongSupplier consumed){
        var contract=lifecycle.require(value.transport(),value.endpoint());
        if(!contract.fields().containsKey(value.field()))throw new IllegalArgumentException("deprecated field is not in lifecycle catalog");
        String key=key(value.transport(),value.endpoint(),value.field());if(entries.putIfAbsent(key,new Entry(value,produced,consumed))!=null)throw new IllegalArgumentException("duplicate field deprecation");
    }
    public void requireCanProduce(ApiOwnershipCatalog.Transport transport,String endpoint,String field,SemanticVersion serverVersion){
        Entry entry=entries.get(key(transport,endpoint,field));if(entry!=null&&serverVersion.compareTo(entry.metadata().stopProducingAt())>=0)throw new IllegalStateException("deprecated field production is disabled");
    }
    public void requireCanConsume(ApiOwnershipCatalog.Transport transport,String endpoint,String field,SemanticVersion serverVersion){
        Entry entry=entries.get(key(transport,endpoint,field));if(entry!=null&&serverVersion.compareTo(entry.metadata().stopConsumingAt())>=0)throw new IllegalStateException("deprecated field consumption is disabled");
    }
    public boolean removable(ApiOwnershipCatalog.Transport transport,String endpoint,String field,SemanticVersion serverVersion){
        Entry entry=entries.get(key(transport,endpoint,field));if(entry==null)return false;
        return serverVersion.major()>=entry.metadata().removeInMajor()&&entry.produced().getAsLong()==0&&entry.consumed().getAsLong()==0;
    }
    public Usage usage(ApiOwnershipCatalog.Transport transport,String endpoint,String field){Entry entry=entries.get(key(transport,endpoint,field));if(entry==null)throw new IllegalArgumentException("field deprecation missing");return new Usage(Math.max(0,entry.produced().getAsLong()),Math.max(0,entry.consumed().getAsLong()));}
    private static String key(ApiOwnershipCatalog.Transport transport,String endpoint,String field){return transport+":"+endpoint+":"+field;}
}
