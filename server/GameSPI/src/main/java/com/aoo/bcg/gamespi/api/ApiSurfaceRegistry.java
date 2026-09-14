package com.aoo.bcg.gamespi.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime one-to-one binding gate from public entry point to its handler. */
public final class ApiSurfaceRegistry {
    public record Binding(ApiOwnershipCatalog.Transport transport,String endpoint,String handlerClass) {
        public Binding { if(transport==null||endpoint==null||endpoint.isBlank()||handlerClass==null||handlerClass.isBlank())throw new IllegalArgumentException("invalid API binding"); }
    }
    private final ApiOwnershipCatalog ownership;private final ApiLifecycleCatalog lifecycle;private final Map<String,Binding> bindings=new LinkedHashMap<>();
    public ApiSurfaceRegistry(ApiOwnershipCatalog ownership,ApiLifecycleCatalog lifecycle){this.ownership=ownership;this.lifecycle=lifecycle;}
    public synchronized void bind(Binding value){ownership.requireOwner(value.transport(),value.endpoint());lifecycle.require(value.transport(),value.endpoint());String key=value.transport()+":"+value.endpoint();if(bindings.putIfAbsent(key,value)!=null)throw new IllegalStateException("shadow API binding rejected: "+key);}
    public synchronized Binding require(ApiOwnershipCatalog.Transport transport,String endpoint){Binding value=bindings.get(transport+":"+endpoint);if(value==null)throw new IllegalStateException("API handler binding missing");return value;}
}
