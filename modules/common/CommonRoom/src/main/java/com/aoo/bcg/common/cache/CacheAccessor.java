package com.aoo.bcg.common.cache;import java.time.Duration;import java.util.Optional;
public interface CacheAccessor{Optional<String>get(String key);void set(String key,String value,Duration ttl);}
