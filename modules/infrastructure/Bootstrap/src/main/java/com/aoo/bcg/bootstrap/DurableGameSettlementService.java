package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.settlement.SettlementBalancePolicy;
import com.aoo.bcg.common.settlement.SettlementEntry;
import com.aoo.bcg.common.settlement.SettlementExecutor;
import com.aoo.bcg.common.settlement.SettlementResult;
import com.aoo.bcg.common.settlement.SettlementScope;
import com.aoo.bcg.gamespi.GameRegistry;
import com.aoo.bcg.gamespi.GameRoomHandle;
import java.util.List;
import java.util.Map;

/** Single durable/idempotent boundary for every game's round settlement. */
public final class DurableGameSettlementService {
    private final GameRegistry games; private final SettlementExecutor settlements;
    public DurableGameSettlementService(GameRegistry games, SettlementExecutor settlements){this.games=java.util.Objects.requireNonNull(games);this.settlements=java.util.Objects.requireNonNull(settlements);}
    public SettlementResult prepare(GameRoomHandle room,int roundNo){var provider=games.require(room.gameId());var payload=provider.settlementProvider().orElseThrow(()->new IllegalStateException("settlement SPI missing: "+provider.descriptor().code())).settle(room,roundNo);if(payload.roomId()!=room.roomId()||payload.roundNo()!=roundNo||!payload.playVersion().equals(room.playVersion()))throw new IllegalStateException("settlement identity mismatch");List<SettlementEntry>entries=payload.scoreDelta().entrySet().stream().map(e->new SettlementEntry(e.getKey(),e.getValue(),Map.of("game",e.getValue()))).toList();return new SettlementResult(room.roomId(),roundNo,room.playVersion(),entries);}
    public SettlementResult persist(SettlementResult result){return settlements.execute(SettlementScope.ROUND,SettlementBalancePolicy.ZERO_SUM,result.roomId(),result.roundNo(),result.playVersion(),result);}
    public SettlementResult settle(GameRoomHandle room,int roundNo){return persist(prepare(room,roundNo));}
}
