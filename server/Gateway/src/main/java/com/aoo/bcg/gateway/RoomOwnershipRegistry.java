package com.aoo.bcg.gateway;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Authoritative room route with TTL, monotonically increasing fencing token and drain/orphan controls. */
public final class RoomOwnershipRegistry {
    public enum NodeState { ACTIVE, DRAINING, DOWN }
    public record Route(long roomId, String incarnationId, String nodeId, long routeVersion,
                        long fencingToken, Instant expiresAt, String protocolVersion) {
        public Route { if(roomId<=0||blank(incarnationId)||blank(nodeId)||routeVersion<=0||fencingToken<=0||expiresAt==null||blank(protocolVersion)) throw new IllegalArgumentException("invalid route"); }
    }
    public record RouteError(GatewayErrorCode code, long roomId, long expectedVersion, String redirectNode, Duration retryAfter) {}
    public record Resolution(Route route, RouteError error) { public boolean found(){return route!=null;} }
    public record TimeoutBudget(Duration gateway, Duration hall, Duration room, Duration ledger, int maxRetries) {
        public TimeoutBudget { if(gateway==null||hall==null||room==null||ledger==null||gateway.isZero()||hall.isZero()||room.isZero()||ledger.isZero()||gateway.isNegative()||hall.isNegative()||room.isNegative()||ledger.isNegative()||maxRetries<0||maxRetries>2||hall.plus(room).plus(ledger).compareTo(gateway)>=0) throw new IllegalArgumentException("invalid layered timeout budget"); }
    }
    private final Map<Long,Route> routes=new ConcurrentHashMap<>(); private final Map<String,NodeState> nodes=new ConcurrentHashMap<>(); private final AtomicLong fence=new AtomicLong();
    public void registerNode(String nodeId){nodes.put(require(nodeId),NodeState.ACTIVE);}
    public void drain(String nodeId){nodes.compute(require(nodeId),(k,v)->{if(v==null)throw new IllegalArgumentException("unknown node");return NodeState.DRAINING;});}
    public Route acquire(long roomId,String incarnationId,String nodeId,String protocolVersion,Duration ttl,Instant now){
        if(nodes.get(nodeId)!=NodeState.ACTIVE)throw new IllegalStateException("node cannot accept new rooms"); if(ttl==null||ttl.isZero()||ttl.isNegative())throw new IllegalArgumentException("positive ttl required");
        Route created=new Route(roomId,require(incarnationId),require(nodeId),1,fence.incrementAndGet(),now.plus(ttl),require(protocolVersion));
        if(routes.putIfAbsent(roomId,created)!=null)throw new IllegalStateException("room route already exists"); return created;
    }
    public Route migrate(long roomId,String target,Duration ttl,Instant now){
        if(nodes.get(target)!=NodeState.ACTIVE)throw new IllegalStateException("target node cannot own rooms");if(ttl==null||ttl.isZero()||ttl.isNegative())throw new IllegalArgumentException("positive ttl required");
        return routes.compute(roomId,(id,old)->{if(old==null)throw new IllegalArgumentException("route not found");return new Route(id,old.incarnationId(),require(target),Math.addExact(old.routeVersion(),1),fence.incrementAndGet(),now.plus(ttl),old.protocolVersion());});
    }
    public Resolution resolve(long roomId,long cachedVersion,String clientProtocol,Instant now){
        Route route=routes.get(roomId); if(route==null)return fail(GatewayErrorCode.ROUTE_NOT_FOUND,roomId,0,"",Duration.ZERO);
        if(!now.isBefore(route.expiresAt())||nodes.get(route.nodeId())==NodeState.DOWN)return fail(GatewayErrorCode.ROUTE_STALE,roomId,route.routeVersion(),"",Duration.ofMillis(100));
        if(!route.protocolVersion().equals(clientProtocol))return fail(GatewayErrorCode.VERSION_INCOMPATIBLE,roomId,route.routeVersion(),"",Duration.ZERO);
        if(cachedVersion>0&&cachedVersion!=route.routeVersion())return fail(GatewayErrorCode.ROUTE_MOVED,roomId,route.routeVersion(),route.nodeId(),Duration.ZERO);
        return new Resolution(route,null);
    }
    public void assertWrite(long roomId,String nodeId,long fencingToken,Instant now){Route r=requireRoute(roomId);if(!r.nodeId().equals(nodeId)||r.fencingToken()!=fencingToken||!now.isBefore(r.expiresAt()))throw new SecurityException(GatewayErrorCode.FENCING_REJECTED.name());}
    public List<Route> orphans(Set<Long> liveRoomIds,Instant now){return routes.values().stream().filter(r->!liveRoomIds.contains(r.roomId())||!now.isBefore(r.expiresAt())||nodes.get(r.nodeId())==NodeState.DOWN).sorted(Comparator.comparingLong(Route::roomId)).toList();}
    public boolean release(long roomId,String incarnationId,long fencingToken){Route r=routes.get(roomId);return r!=null&&r.incarnationId().equals(incarnationId)&&r.fencingToken()==fencingToken&&routes.remove(roomId,r);}
    public void nodeDown(String nodeId){nodes.put(require(nodeId),NodeState.DOWN);}
    private Route requireRoute(long id){Route r=routes.get(id);if(r==null)throw new IllegalArgumentException("route not found");return r;}
    private static Resolution fail(GatewayErrorCode code,long id,long version,String node,Duration retry){return new Resolution(null,new RouteError(code,id,version,node,retry));}
    private static String require(String s){if(blank(s))throw new IllegalArgumentException("value required");return s;}
    private static boolean blank(String s){return s==null||s.isBlank();}
}
