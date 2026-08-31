package business.player.feature;

import business.player.Player;
import business.shareplayer.SharePlayerWalletMgr;
import cenum.ConstEnum;
import cenum.ItemFlow;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.DistributedRedisLock;
import core.db.entity.clarkGame.PlayerWalletBO;
import core.db.service.clarkGame.PlayerWalletBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionDefine;

import java.util.UUID;

/**
 * Account-wide room-card wallet. Region and city values never partition a balance.
 *
 * <p>Historical region-partitioned rows are merged by Flyway before this feature starts.</p>
 */
public final class PlayerWallet extends Feature {
    private static final int MAX_BALANCE = 1_999_999_999;
    private PlayerWalletBO wallet;

    public PlayerWallet(Player player) {
        super(player);
    }

    @Override
    public void loadDB() {
        walletRecord();
    }

    public int activateWallet() {
        return balance();
    }

    public boolean check(int value) {
        return value >= 0 && balance() >= value;
    }

    public boolean checkAndConsumeRoom(int count, GameType gameType) {
        return check(count) && consumeRoomCard(count, gameType.getId(), ItemFlow.RoomCardRoom,
                ConstEnum.ResOpType.Lose);
    }

    public boolean checkAndConsumeItemFlow(int count, ItemFlow reason) {
        return check(count) && consumeRoomCard(count, -1, reason, ConstEnum.ResOpType.Lose);
    }

    public void gainItemFlow(int count, ItemFlow reason) {
        gainRoomCard(count, -1, reason, ConstEnum.ResOpType.Gain);
    }

    public boolean checkAndClubConsumeRoom(int count, GameType gameType, long clubId,
                                           Club_define.Club_OperationStatus status,
                                           long agentsId, int level) {
        if (check(count) == false) return false;
        Mutation mutation = mutate(-count, gameType.getId(), ItemFlow.RoomCardClubRoom,
                ConstEnum.ResOpType.Lose);
        if (mutation == null) return false;
        FlowLogger.clubRoomCardChargeLog(player.getPid(), clubId, status.value(),
                ItemFlow.RoomCardClubRoom.value(), mutation.delta(), mutation.after(), mutation.before(),
                ConstEnum.ResOpType.Lose.ordinal(), gameType.getId(), 0, agentsId, level, 0);
        return true;
    }

    public void backClubConsumeRoom(int count, GameType gameType, long clubId,
                                    Club_define.Club_OperationStatus status,
                                    long agentsId, int level) {
        if (count <= 0) return;
        Mutation mutation = mutate(count, gameType.getId(), ItemFlow.RoomCardClubRoom,
                ConstEnum.ResOpType.Fallback);
        FlowLogger.clubRoomCardChargeLog(player.getPid(), clubId, status.value(),
                ItemFlow.RoomCardClubRoom.value(), mutation.delta(), mutation.after(), mutation.before(),
                ConstEnum.ResOpType.Fallback.ordinal(), gameType.getId(), 0, agentsId, level,
                0);
    }

    public boolean checkAndUnionConsumeRoom(int count, GameType gameType, long unionId,
                                            UnionDefine.UNION_OPERATION_STATUS status,
                                            long agentsId, int level) {
        if (check(count) == false) return false;
        Mutation mutation = mutate(-count, gameType.getId(), ItemFlow.RoomCardUnionRoom,
                ConstEnum.ResOpType.Lose);
        if (mutation == null) return false;
        logUnion(mutation, gameType, unionId, status, agentsId, level, ConstEnum.ResOpType.Lose);
        return true;
    }

    public void backUnionConsumeRoom(int count, GameType gameType, long unionId,
                                     UnionDefine.UNION_OPERATION_STATUS status,
                                     long agentsId, int level) {
        if (count <= 0) return;
        Mutation mutation = mutate(count, gameType.getId(), ItemFlow.RoomCardUnionRoom,
                ConstEnum.ResOpType.Fallback);
        logUnion(mutation, gameType, unionId, status, agentsId, level, ConstEnum.ResOpType.Fallback);
    }

    public void backConsumeRoom(int count, GameType gameType) {
        gainRoomCard(count, gameType.getId(), ItemFlow.RoomCardRoom, ConstEnum.ResOpType.Fallback);
    }

    public void backConsumeRoom(int count, GameType gameType, int several) {
        if (several <= 0) throw new IllegalArgumentException("several must be positive");
        int refund = count - (int) Math.ceil(count * 1.0 / several);
        backConsumeRoom(refund, gameType);
    }

    public void roomCardRefererReward(int count) {
        gainRoomCard(count, -1, ItemFlow.RefererReward, ConstEnum.ResOpType.Gain);
    }

    public int balance() {
        return walletRecord().getValue();
    }

    public boolean consumeRoomCard(int value, int gameId, ItemFlow reason,
                                   ConstEnum.ResOpType resOpType) {
        return value > 0 && mutate(-value, gameId, reason, resOpType) != null;
    }

    public int gainRoomCard(int value, int gameId, ItemFlow reason,
                            ConstEnum.ResOpType resOpType) {
        if (value <= 0) return 0;
        return mutate(value, gameId, reason, resOpType).delta();
    }

    private void logUnion(Mutation mutation, GameType gameType, long unionId,
                          UnionDefine.UNION_OPERATION_STATUS status,
                          long agentsId, int level, ConstEnum.ResOpType operation) {
        FlowLogger.unionRoomCardChargeLog(player.getPid(), unionId, status.value(),
                ItemFlow.RoomCardUnionRoom.value(), mutation.delta(), mutation.after(), mutation.before(),
                operation.ordinal(), operation.ordinal(), 0, agentsId, level, 0);
    }

    private Mutation mutate(int requestedDelta, int gameId, ItemFlow reason,
                            ConstEnum.ResOpType resOpType) {
        String lockOwner = UUID.randomUUID().toString();
        String lockKey = "playerWallet:" + getPid();
        try {
            DistributedRedisLock.acquire(lockKey, lockOwner);
            PlayerWalletBO wallet = walletRecord();
            this.lock();
            try {
                int before = wallet.getValue();
                if (requestedDelta < 0 && before < -requestedDelta) return null;
                int after = (int) Math.max(0L, Math.min(MAX_BALANCE, (long) before + requestedDelta));
                wallet.saveValue(after);
                if (Config.isShare()) SharePlayerWalletMgr.getInstance().add(wallet);
                getPlayer().pushProperties("roomCard", after);
                int applied = after - before;
                FlowLogger.roomCardChargeLog(player.getPid(), reason.value(), applied, after, before,
                        resOpType.ordinal(), player.getFamiliID(), gameId, 0, 0);
                return new Mutation(before, after, applied);
            } finally {
                this.unlock();
            }
        } finally {
            DistributedRedisLock.release(lockKey, lockOwner);
        }
    }

    private synchronized PlayerWalletBO walletRecord() {
        if (wallet != null) return wallet;
        String owner = UUID.randomUUID().toString();
        String migrationLock = "playerWalletMigration:" + getPid();
        try {
            DistributedRedisLock.acquire(migrationLock, owner);
            PlayerWalletBOService service = ContainerMgr.get().getComponent(PlayerWalletBOService.class);
            PlayerWalletBO current = service.findByPid(getPid());
            if (current == null && Config.isShare()) current = SharePlayerWalletMgr.getInstance().get(getPid());
            if (current == null) {
                current = new PlayerWalletBO(getPid());
                long id = service.saveIgnoreOrUpDate(current);
                if (current.getId() <= 0) current.setId(id);
            }
            int embeddedLegacyBalance = Math.max(0, getPlayer().getPlayerBO().getRoomCard());
            if (embeddedLegacyBalance > 0) {
                current.saveValue((int) Math.min(MAX_BALANCE, (long) current.getValue() + embeddedLegacyBalance));
                getPlayer().getPlayerBO().saveRoomCard(0);
            }
            wallet = current;
            if (Config.isShare()) SharePlayerWalletMgr.getInstance().add(current);
            return current;
        } finally {
            DistributedRedisLock.release(migrationLock, owner);
        }
    }

    private record Mutation(int before, int after, int delta) {
    }
}
