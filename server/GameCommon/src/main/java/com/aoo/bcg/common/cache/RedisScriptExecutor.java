package com.aoo.bcg.common.cache;import java.util.List;
@FunctionalInterface public interface RedisScriptExecutor{long eval(String script,List<String>keys,List<String>args);}
