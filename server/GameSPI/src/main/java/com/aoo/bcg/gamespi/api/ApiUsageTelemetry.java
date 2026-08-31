package com.aoo.bcg.gamespi.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Bounded-cardinality call counters keyed by catalogued endpoint and semantic client version. */
public final class ApiUsageTelemetry {
    public record Key(ApiOwnershipCatalog.Transport transport,String endpoint,SemanticVersion clientVersion) {}
    private final ApiLifecycleCatalog lifecycle;
    private final ConcurrentHashMap<Key,LongAdder> calls=new ConcurrentHashMap<>();
    private final int maximumSeries;
    public ApiUsageTelemetry(ApiLifecycleCatalog lifecycle,int maximumSeries){if(maximumSeries<1)throw new IllegalArgumentException("maximum series must be positive");this.lifecycle=lifecycle;this.maximumSeries=maximumSeries;}
    public void record(ApiOwnershipCatalog.Transport transport,String endpoint,SemanticVersion clientVersion){
        lifecycle.require(transport,endpoint);Key key=new Key(transport,endpoint,clientVersion);
        LongAdder existing=calls.get(key);if(existing!=null){existing.increment();return;}
        if(calls.size()>=maximumSeries)throw new IllegalStateException("API usage metric cardinality limit reached");
        calls.computeIfAbsent(key,ignored->new LongAdder()).increment();
    }
    public Map<Key,Long> snapshot(){Map<Key,Long> result=new LinkedHashMap<>();calls.forEach((key,value)->result.put(key,value.sum()));return Map.copyOf(result);}
}
