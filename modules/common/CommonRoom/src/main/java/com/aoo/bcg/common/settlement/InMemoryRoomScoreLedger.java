package com.aoo.bcg.common.settlement;

import com.aoo.bcg.gamespi.RoomScoreLedger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic reference implementation used by provider tests and local simulations. */
public final class InMemoryRoomScoreLedger implements RoomScoreLedger {
    private final Map<Long, RoomState> rooms = new ConcurrentHashMap<>();
    private final Map<String, Receipt> receipts = new ConcurrentHashMap<>();

    public void seed(long roomId, Map<Long, Long> balances) {
        if (rooms.putIfAbsent(roomId, new RoomState(0, new LinkedHashMap<>(balances))) != null) {
            throw new IllegalStateException("room score ledger already seeded");
        }
        snapshot(roomId, balances.keySet());
    }

    @Override public Snapshot snapshot(long roomId, Collection<Long> playerIds) {
        RoomState room = requireRoom(roomId);
        synchronized (room) {
            Map<Long, Long> selected = new LinkedHashMap<>();
            for (Long playerId : playerIds) {
                Long balance = room.balances.get(playerId);
                if (balance == null) throw new IllegalStateException("authoritative score balance missing");
                selected.put(playerId, balance);
            }
            return new Snapshot(roomId, room.revision, selected);
        }
    }

    @Override public Receipt commit(Command command) {
        Receipt previous = receipts.get(command.operationId());
        if (previous != null) return replay(previous, command);
        RoomState room = requireRoom(command.roomId());
        synchronized (room) {
            previous = receipts.get(command.operationId());
            if (previous != null) return replay(previous, command);
            if (room.revision != command.expectedRevision()) throw new IllegalStateException("score snapshot revision changed");
            for (var entry : command.expectedBalances().entrySet()) {
                if (!entry.getValue().equals(room.balances.get(entry.getKey()))) {
                    throw new IllegalStateException("score balance changed");
                }
            }
            Map<Long, Long> next = new LinkedHashMap<>(room.balances);
            command.deltas().forEach((playerId, delta) -> {
                long updated = Math.addExact(next.getOrDefault(playerId, -1L), delta);
                if (updated < 0) throw new IllegalStateException("insufficient score balance");
                next.put(playerId, updated);
            });
            room.balances.clear(); room.balances.putAll(next); room.revision++;
            Receipt receipt = new Receipt(command.operationId(), command.roomId(), command.roundNo(),
                    room.revision, select(next, command.deltas().keySet()), command.deltas(), false);
            receipts.put(command.operationId(), receipt);
            return receipt;
        }
    }

    private Receipt replay(Receipt receipt, Command command) {
        if (receipt.roomId() != command.roomId() || receipt.roundNo() != command.roundNo()
                || !receipt.deltas().equals(command.deltas())) {
            throw new IllegalArgumentException("score operationId reused with different settlement");
        }
        return new Receipt(receipt.operationId(), receipt.roomId(), receipt.roundNo(), receipt.revision(),
                receipt.balances(), receipt.deltas(), true);
    }

    private RoomState requireRoom(long roomId) {
        RoomState state = rooms.get(roomId);
        if (state == null) throw new IllegalStateException("room score ledger unavailable");
        return state;
    }

    private static Map<Long, Long> select(Map<Long, Long> values, Collection<Long> ids) {
        Map<Long, Long> selected = new LinkedHashMap<>();
        ids.forEach(id -> selected.put(id, values.get(id)));
        return selected;
    }

    private static final class RoomState {
        private long revision;
        private final Map<Long, Long> balances;
        private RoomState(long revision, Map<Long, Long> balances) { this.revision = revision; this.balances = balances; }
    }
}
