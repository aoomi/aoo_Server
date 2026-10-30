package com.aoo.bcg.lobby;

import java.util.*;

/** Opt-in, one-shot, loopback-safe diagnostic projection for the 2001 create route. */
public final class LobbyCreateDiagnostic {
    private LobbyCreateDiagnostic() {}
    public static boolean enabled() {
        return Boolean.getBoolean("aoo.hall.clubCreateDiagnostic2001.enabled");
    }
    public static void recordStatus(int status) { if (enabled()) System.err.println("[LobbyCreateDiagnostic] httpStatus="+status); }
    public static boolean loopback(String host) { return "127.0.0.1".equals(host) || "::1".equals(host) || "localhost".equalsIgnoreCase(host); }
    public static Map<String,Object> project(long gameId, Map<String,Object> request,
                                              Map<String,Object> response, int status) {
        return project(gameId, request, response, status, "127.0.0.1");
    }
    public static Map<String,Object> project(long gameId, Map<String,Object> request,
                                              Map<String,Object> response, int status, String remoteHost) {
        if (gameId != 2001 || !enabled() || !loopback(remoteHost)) return Map.of();
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("gameId", 2001L);
        out.put("requestKeyNames", sortedNames(request));
        out.put("playVersion", string(response, "playVersion", request));
        out.put("releaseId", number(response, "releaseId", request));
        out.put("indexGeneration", number(response, "indexGeneration", request));
        out.put("sourceHash", safeHash(response.get("sourceHash"), request.get("sourceHash")));
        out.put("httpStatus", status);
        Object rules = response.get("rules");
        if (!(rules instanceof Map<?,?>)) rules = request.get("rules");
        out.put("ruleKeyNames", sortedNames(rules instanceof Map<?,?> m ? cast(m) : Map.of()));
        out.put("ruleValueTypes", valueTypes(rules instanceof Map<?,?> m ? cast(m) : Map.of()));
        return Map.copyOf(out);
    }
    private static String string(Map<String,Object> response,String key,Map<String,Object> request){
        Object v=response.get(key); if (!(v instanceof String)) v=request.get(key); return v instanceof String s ? s : null;
    }
    private static Object number(Map<String,Object> response,String key,Map<String,Object> request){
        Object v=response.get(key); if (!(v instanceof Number)) v=request.get(key); return v instanceof Number n ? n : null;
    }
    private static String safeHash(Object a,Object b){Object v=a instanceof String?safeHashString(a):b;return v instanceof String s&&s.matches("[0-9a-fA-F]{64}")?s:null;}
    private static Object safeHashString(Object v){return v;}
    private static List<String> sortedNames(Map<String,Object> m){return m.keySet().stream().filter(Objects::nonNull).map(String::valueOf).filter(LobbyCreateDiagnostic::safeKey).sorted().toList();}
    private static Map<String,String> valueTypes(Map<String,Object> m){Map<String,String> out=new TreeMap<>();for(var e:m.entrySet())if(safeKey(String.valueOf(e.getKey())))out.put(String.valueOf(e.getKey()),type(e.getValue()));return Map.copyOf(out);}
    private static boolean safeKey(String k){String x=k.toLowerCase(Locale.ROOT);return !(x.contains("token")||x.contains("password")||x.contains("user")||x.contains("hand")||x.contains("card"));}
    private static String type(Object v){if(v==null)return "null";if(v instanceof Map)return "object";if(v instanceof List)return "array";if(v instanceof Boolean)return "boolean";if(v instanceof Number)return "number";return "string";}
    @SuppressWarnings("unchecked") private static Map<String,Object> cast(Map<?,?> m){Map<String,Object> out=new LinkedHashMap<>();for(var e:m.entrySet())if(e.getKey()!=null)out.put(String.valueOf(e.getKey()),e.getValue());return out;}
}
