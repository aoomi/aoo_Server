package com.aoo.bcg.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Append-only public error-code ledger. Published numbers and meanings are permanent. */
public final class ErrorCodeCatalog {
    public record Definition(int code,String symbolicName,String meaning,String introducedVersion,String retiredVersion){
        public Definition{
            if(code<1000||code>6999||symbolicName==null||!symbolicName.matches("[A-Z][A-Z0-9_]+")
                    ||meaning==null||meaning.isBlank()||introducedVersion==null||!introducedVersion.matches("\\d+\\.\\d+\\.\\d+")
                    ||(retiredVersion!=null&&!retiredVersion.matches("\\d+\\.\\d+\\.\\d+")))throw new IllegalArgumentException("invalid error-code definition");
        }
        Definition retire(String version){if(retiredVersion!=null)throw new IllegalStateException("error code already retired");return new Definition(code,symbolicName,meaning,introducedVersion,version);}
    }
    private final Map<Integer,Definition> definitions=new LinkedHashMap<>();
    public synchronized void publish(Definition candidate){
        Definition existing=definitions.get(candidate.code());
        if(existing!=null&&!existing.equals(candidate))throw new IllegalStateException("published error code cannot change meaning or be reused: "+candidate.code());
        if(existing==null&&definitions.values().stream().anyMatch(value->value.symbolicName().equals(candidate.symbolicName())))throw new IllegalStateException("error symbolic name cannot be reused");
        definitions.putIfAbsent(candidate.code(),candidate);
    }
    public synchronized void retire(int code,String version){Definition value=require(code);definitions.put(code,value.retire(version));}
    public synchronized Definition require(int code){Definition value=definitions.get(code);if(value==null)throw new IllegalArgumentException("unpublished error code: "+code);return value;}
    public synchronized Map<Integer,Definition> snapshot(){return Map.copyOf(definitions);}
    public static ErrorCodeCatalog standard(){
        var catalog=new ErrorCodeCatalog();
        catalog.publish(new Definition(1008,"DATABASE_UNAVAILABLE","数据库服务暂时不可用","2.0.0",null));
        catalog.publish(new Definition(1009,"DUPLICATE_SUBMISSION","数据已存在，请勿重复提交","2.0.0",null));
        catalog.publish(new Definition(1010,"CONCURRENT_CONFLICT","请求发生并发冲突，请重试","2.0.0",null));
        catalog.publish(new Definition(1011,"BUSINESS_CONSTRAINT","数据不符合业务约束","2.0.0",null));
        catalog.publish(new Definition(1012,"OPERATION_TIMEOUT","请求处理超时，请重试","2.0.0",null));
        catalog.publish(new Definition(2001,"UNAUTHORIZED","登录状态无效","2.0.0",null));
        catalog.publish(new Definition(3001,"ROOM_NOT_FOUND","房间不存在","2.0.0",null));
        catalog.publish(new Definition(4001,"ILLEGAL_GAME_OPERATION","当前操作不合法","2.0.0",null));
        return catalog;
    }
}
