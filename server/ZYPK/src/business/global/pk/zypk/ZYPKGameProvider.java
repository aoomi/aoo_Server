package business.global.pk.zypk;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameCommandCommitter;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.StrictEnumDecoder;
import com.aoo.bcg.poker.PokerGameProvider;
import com.aoo.bcg.poker.ConfigurablePokerFamily;
import com.aoo.bcg.poker.PokerRuleFamily;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Optional;

public final class ZYPKGameProvider implements PokerGameProvider {
    private static final GameDescriptor DESCRIPTOR = new GameDescriptor(
            62, "zypk", "自由扑克", GameCategory.POKER,
            ConfigurablePokerFamily.CODE, RegionScope.NATIONAL, "", "", "zypk-v1.0.0");
    private static final SecureRandom SEEDS = new SecureRandom();

    @Override public GameDescriptor descriptor() { return DESCRIPTOR; }
    @Override public GameRoomFactory roomFactory() { return ZYPKGameProvider::createRoom; }
    @Override public PokerRuleFamily pokerFamily() { return new ConfigurablePokerFamily(); }
    @Override public Optional<GameCommandHandler> commandHandler() { return Optional.of(new ZYPKCommandHandler()); }
    @Override public GameCommandCommitter commandCommitter() { return new ZYPKProductionCommitter(); }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->room.requireLegacyRoom(ZYPKTable.class).reconnectView(viewer));}
    @Override public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->{var result=room.requireLegacyRoom(ZYPKTable.class).settlement(round);java.util.Map<Long,Long>deltas=new java.util.LinkedHashMap<>();result.entries().forEach(entry->deltas.put(entry.playerId(),entry.scoreDelta()));return new com.aoo.bcg.gamespi.SettlementPayload(room.roomId(),round,room.playVersion(),deltas);});}

    private static GameRoomHandle createRoom(RoomCreationContext context) {
        ZYPKTable table = new ZYPKTable(context.roomId(), context.ownerId(),
                intRule(context, "seatLimit", 4), intRule(context, "privateCardCount", 5),
                intRule(context, "reserveCardCount", 0), cardSet(context, "excludedCards"),
                actionSet(context), intRule(context, "initialChipCount", intRule(context, "chouMa", 0)),
                operationMode(context),
                SEEDS.nextLong());
        return new GameRoomHandle(context.roomId(), DESCRIPTOR.gameId(), DESCRIPTOR.version(), table);
    }

    private static int intRule(RoomCreationContext context, String key, int fallback) {
        Object value = context.immutableRules().get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
    private static long longRule(RoomCreationContext context, String key, long fallback) {
        Object value = context.immutableRules().get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }
    private static ZYPKTable.OperationMode operationMode(RoomCreationContext context) {
        Object value = context.immutableRules().get("operationMode");
        if (value == null) value = context.immutableRules().get("moShi");
        if (value instanceof Number number)
            return number.intValue() == 1 ? ZYPKTable.OperationMode.SIMULTANEOUS : ZYPKTable.OperationMode.TURN_BASED;
        if (value == null) return ZYPKTable.OperationMode.TURN_BASED;
        String name = String.valueOf(value).trim();
        if (name.equalsIgnoreCase("Tong") || name.equalsIgnoreCase("SIMULTANEOUS"))
            return ZYPKTable.OperationMode.SIMULTANEOUS;
        if (name.equalsIgnoreCase("Luan") || name.equalsIgnoreCase("TURN_BASED"))
            return ZYPKTable.OperationMode.TURN_BASED;
        throw new IllegalArgumentException("unsupported ZYPK operationMode: " + value);
    }
    private static Set<Integer> cardSet(RoomCreationContext context, String key) {
        Object value = context.immutableRules().get(key);
        if (!(value instanceof Collection<?> values)) return Set.of();
        Set<Integer> cards = new LinkedHashSet<>();
        for (Object item : values) {
            if (!(item instanceof Number number)) throw new IllegalArgumentException(key + " must contain numbers");
            cards.add(number.intValue());
        }
        return cards;
    }
    private static Set<jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu> actionSet(RoomCreationContext context) {
        Object value = context.immutableRules().get("enabledActions");
        if (!(value instanceof Collection<?> values)) {
            java.util.EnumSet<jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu> defaults =
                    java.util.EnumSet.allOf(jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu.class);
            defaults.remove(jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu.Not);
            return defaults;
        }
        Set<jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu> actions = new LinkedHashSet<>();
        for (Object item : values) actions.add(item instanceof Number
                ? StrictEnumDecoder.byCode(jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu.class,
                        item, jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu::value)
                : StrictEnumDecoder.byName(jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu.class, item));
        return actions;
    }
}
