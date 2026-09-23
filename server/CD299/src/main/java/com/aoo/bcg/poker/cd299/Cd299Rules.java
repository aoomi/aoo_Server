package com.aoo.bcg.poker.cd299;

import java.util.Map;

/** Immutable, gameplay-only room rules for 成都扯旋. */
public record Cd299Rules(int roomDurationMinutes, int roundLimit, int maxPlayers, int startPlayers,
        int operationSeconds, StandPolicy standPolicy, FlipMode flipMode, int mangoRaise,
        int mangoScore, int openingBet, int splitSeconds, int splitExtensionSeconds,
        int splitExtensionLimit, int seatConfirmationSeconds, int seatRetentionSeconds,
        int autoOperateAfterTimeouts, boolean spectatorEntry, boolean autoReady,
        boolean supplementNextRound, boolean mute, boolean interactionDisabled,
        boolean distanceTips, boolean restMango, boolean beatMango,
        boolean everyHandMango, boolean firstRoundCanRest, boolean eachPlayerMustFollow,
        boolean earthNineKing, boolean fireproofCard, boolean headBigKeepsBase) {
    public static final String GAME_CODE = "CD299";
    public static final String FAMILY = "poker:cd299";
    public static final String VERSION = "cd299-v1.0.0";
    public enum StandPolicy { LOSER_ONLY, EVERYONE, NOBODY }
    public enum FlipMode { LADDER, ROLLING }

    public Cd299Rules {
        if ((roomDurationMinutes != 30 && roomDurationMinutes != 45 && roomDurationMinutes != 60) || roundLimit != 10
                || maxPlayers != 8 || (startPlayers != 2 && startPlayers != 4 && startPlayers != 6)
                || startPlayers > maxPlayers || (operationSeconds != 10 && operationSeconds != 15 && operationSeconds != 30)
                || mangoRaise < 0 || mangoRaise > 5 || mangoScore < 0 || openingBet < mangoScore
                || splitSeconds != 30 || splitExtensionSeconds != 30 || splitExtensionLimit != 1
                || seatConfirmationSeconds != 30 || seatRetentionSeconds != 120
                || autoOperateAfterTimeouts != 3)
            throw new IllegalArgumentException("invalid CD299 rules");
    }
    public static Cd299Rules from(Map<String, ?> raw) {
        Map<String, ?> r = raw == null ? Map.of() : raw;
        return new Cd299Rules(number(r,"roomDurationMinutes",30),number(r,"roundLimit",10),8,number(r,"startPlayers",2),
                number(r,"operationSeconds",10),enumValue(r,"standPolicy",StandPolicy.class,StandPolicy.LOSER_ONLY),
                enumValue(r,"mangoFlipMode",FlipMode.class,FlipMode.LADDER),number(r,"mangoRaise",3),
                number(r,"mangoScore",3),number(r,"openingBet",3),number(r,"splitSeconds",30),
                number(r,"splitExtensionSeconds",30),number(r,"splitExtensionLimit",1),
                number(r,"seatConfirmationSeconds",30),number(r,"seatRetentionSeconds",120),
                number(r,"autoOperateAfterTimeouts",3),bool(r,"spectatorEntry",true),
                bool(r,"autoReady",true),bool(r,"supplementNextRound",true),bool(r,"mute",true),
                bool(r,"interactionDisabled",false),bool(r,"distanceTips",true),
                bool(r,"restMango",true),bool(r,"beatMango",true),bool(r,"everyHandMango",true),
                bool(r,"firstRoundCanRest",true),bool(r,"eachPlayerMustFollow",false),
                bool(r,"earthNineKing",true),bool(r,"fireproofCard",true),bool(r,"bigHeadKeepsBase",false));
    }
    public Map<String,Object> toMap(){return Map.ofEntries(
            Map.entry("roomDurationMinutes",roomDurationMinutes),Map.entry("roundLimit",roundLimit),Map.entry("maxPlayers",maxPlayers),
            Map.entry("startPlayers",startPlayers),Map.entry("operationSeconds",operationSeconds),
            Map.entry("standPolicy",standPolicy.name()),Map.entry("mangoFlipMode",flipMode.name()),
            Map.entry("mangoRaise",mangoRaise),Map.entry("mangoScore",mangoScore),
            Map.entry("openingBet",openingBet),Map.entry("splitSeconds",splitSeconds),
            Map.entry("splitExtensionSeconds",splitExtensionSeconds),Map.entry("splitExtensionLimit",splitExtensionLimit),
            Map.entry("seatConfirmationSeconds",seatConfirmationSeconds),Map.entry("seatRetentionSeconds",seatRetentionSeconds),
            Map.entry("autoOperateAfterTimeouts",autoOperateAfterTimeouts),Map.entry("spectatorEntry",spectatorEntry),
            Map.entry("autoReady",autoReady),Map.entry("supplementNextRound",supplementNextRound),
            Map.entry("mute",mute),Map.entry("interactionDisabled",interactionDisabled),Map.entry("distanceTips",distanceTips),
            Map.entry("restMango",restMango),Map.entry("beatMango",beatMango),
            Map.entry("everyHandMango",everyHandMango),Map.entry("firstRoundCanRest",firstRoundCanRest),
            Map.entry("eachPlayerMustFollow",eachPlayerMustFollow),Map.entry("earthNineKing",earthNineKing),
            Map.entry("fireproofCard",fireproofCard),Map.entry("bigHeadKeepsBase",headBigKeepsBase));}
    private static int number(Map<String, ?> r,String key,int fallback){Object v=r.get(key);if(v==null)return fallback;if(v instanceof Number n&&n.doubleValue()==n.intValue())return n.intValue();throw new IllegalArgumentException("invalid "+key);}
    private static boolean bool(Map<String,?>r,String key,boolean fallback){Object v=r.get(key);if(v==null)return fallback;if(v instanceof Boolean b)return b;throw new IllegalArgumentException("invalid "+key);}
    private static <E extends Enum<E>> E enumValue(Map<String,?>r,String key,Class<E>type,E fallback){Object v=r.get(key);if(v==null)return fallback;try{return Enum.valueOf(type,String.valueOf(v));}catch(IllegalArgumentException e){throw new IllegalArgumentException("invalid "+key,e);}}
}
