package com.aoo.bcg.gamespi.api;

import java.util.Set;

/** Synchronized deny-list for retired routes, messages, listeners and configuration switches. */
public final class DeprecatedEntryPointBlocklist {
    private static final Set<String> HTTP_PREFIXES=Set.of("/v1/","/api/v2/","/ClientPack","/legacy/","/api.php");
    private static final Set<String> MESSAGE_PREFIXES=Set.of("legacy.","v1.","old.");
    private static final Set<String> RETIRED_MESSAGES=Set.of("room.CBaseRoomXiPai","room.CBaseKickRoom","club.CClubKickRoom","union.CUnionKickRoom");
    private static final Set<Integer> NETWORK_PORTS=Set.of(904,9998);
    private static final Set<String> CONFIG_KEYS=Set.of("legacy.protocol.enabled","old.gateway.enabled","old.handler.path");
    private DeprecatedEntryPointBlocklist(){}
    public static void requireMessageAllowed(String messageId){if(messageId==null||RETIRED_MESSAGES.contains(messageId)||MESSAGE_PREFIXES.stream().anyMatch(messageId::startsWith)||messageId.matches("\\d+"))throw new SecurityException("retired message entry point is blocked");}
    public static void requireHttpAllowed(String path){if(path==null||HTTP_PREFIXES.stream().anyMatch(path::startsWith))throw new SecurityException("retired HTTP entry point is blocked");}
    public static void requirePortAllowed(int port){if(NETWORK_PORTS.contains(port))throw new SecurityException("retired network listener is blocked");}
    public static void requireConfigAllowed(String key){if(CONFIG_KEYS.contains(key))throw new SecurityException("retired configuration switch is blocked");}
}
