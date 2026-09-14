package com.aoo.bcg.gamespi.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Unified behavior change log consumed by frontend, operations and customer support. */
public final class ApiChangeLog {
    public enum Audience { FRONTEND, OPERATIONS, SUPPORT }
    public enum ChangeType { ADDED, CHANGED, DEPRECATED, REMOVED, SECURITY }
    public record Entry(SemanticVersion version,LocalDate effectiveDate,ApiOwnershipCatalog.Transport transport,
                        String endpoint,ChangeType type,String behavior,Set<Audience> audiences,String migration){
        public Entry{if(version==null||effectiveDate==null||transport==null||endpoint==null||endpoint.isBlank()||type==null
                ||behavior==null||behavior.isBlank()||audiences==null||audiences.isEmpty()||migration==null||migration.isBlank())throw new IllegalArgumentException("incomplete API change log entry");audiences=Set.copyOf(audiences);}
    }
    private final List<Entry> entries;
    public ApiChangeLog(List<Entry> entries){this.entries=entries.stream().sorted(java.util.Comparator.comparing(Entry::effectiveDate).reversed()).toList();}
    public List<Entry> query(Audience audience,SemanticVersion since){return entries.stream().filter(value->value.audiences().contains(audience)&&value.version().compareTo(since)>=0).toList();}
    public List<Entry> endpoint(String endpoint){return entries.stream().filter(value->value.endpoint().equals(endpoint)).toList();}
    public static ApiChangeLog standard(){var all=java.util.EnumSet.allOf(Audience.class);return new ApiChangeLog(List.of(
        new Entry(SemanticVersion.parse("2.0.0"),LocalDate.parse("2026-08-23"),ApiOwnershipCatalog.Transport.WSS,"common.*",ChangeType.SECURITY,"游戏权威操作统一使用带序号、幂等号和玩法版本的 V2 帧",all,"客户端切换至 *_req；运维封锁旧监听；客服提示升级客户端"),
        new Entry(SemanticVersion.parse("2.0.0"),LocalDate.parse("2026-08-23"),ApiOwnershipCatalog.Transport.HTTP,"/api/v2/admin/game-profiles",ChangeType.ADDED,"玩法版本发布与回滚进入独立管理控制面",all,"使用独立管理域名和发布权限")));
    }
}
