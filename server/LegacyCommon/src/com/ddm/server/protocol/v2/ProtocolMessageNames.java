package com.ddm.server.protocol.v2;

import java.util.Locale;

public final class ProtocolMessageNames {
    private ProtocolMessageNames() {}

    public static String canonical(String event) {
        String snake = event.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[._-]+", "_").toLowerCase(Locale.ROOT);
        String plain = snake.replaceFirst("^[sc]", "");
        if (plain.matches(".*(login|token|account|auth).*$")) return "account." + plain;
        if (plain.matches(".*(club|union).*$")) return "club." + plain;
        if (plain.contains("pdk")) return "poker.pdk." + plain.replaceFirst("^pdk_?", "");
        if (plain.contains("cdxzmj") || plain.contains("xuezhan")) {
            return "mahjong.xuezhan." + plain.replaceFirst("^cdxzmj_?", "");
        }
        if (plain.matches(".*(room|ready|dissolve|trusteeship|chat|voice|gift).*$")) {
            return "common.room." + plain;
        }
        return "hall." + plain;
    }

    public static String canonicalPush(String event) {
        String request = canonical(event);
        if (request.startsWith("account.")) return "account.session_push";
        if (request.startsWith("club.")) return "club.state_push";
        if (request.startsWith("common.room.")) return "common.room.compat_state_push";
        if (request.startsWith("poker.pdk.")) return "poker.pdk.state_push";
        if (request.startsWith("mahjong.xuezhan.")) return "mahjong.xuezhan.state_push";
        if (request.startsWith("game.")) return "game.state_push";
        return "hall.state_push";
    }
}
