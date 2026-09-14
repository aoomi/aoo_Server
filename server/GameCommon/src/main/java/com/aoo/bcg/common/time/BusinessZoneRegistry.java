package com.aoo.bcg.common.time;
import java.time.ZoneId;import java.util.Map;
public final class BusinessZoneRegistry{
 private static final Map<String,ZoneId>ZONES=Map.of("CN",ZoneId.of("Asia/Shanghai"),"HK",ZoneId.of("Asia/Hong_Kong"),"UTC",ZoneId.of("UTC"));
 private BusinessZoneRegistry(){}
 public static ZoneId require(String region){ZoneId zone=ZONES.get(region);if(zone==null)throw new IllegalArgumentException("unsupported business region: "+region);return zone;}
}
