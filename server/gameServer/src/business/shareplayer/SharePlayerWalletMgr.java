package business.shareplayer;

import com.ddm.server.common.redis.RedisMap;
import com.google.gson.Gson;
import core.db.entity.clarkGame.PlayerWalletBO;
import core.ioc.ContainerMgr;

/** Shared cache for the single account-wide wallet row. */
public final class SharePlayerWalletMgr {
    private static final SharePlayerWalletMgr INSTANCE = new SharePlayerWalletMgr();
    private static final String KEY_PREFIX = "sharePlayerWallet:";
    private static final String BALANCE_KEY = "wallet";

    public static SharePlayerWalletMgr getInstance() { return INSTANCE; }

    public void add(PlayerWalletBO wallet) {
        map(wallet.getPid()).put(BALANCE_KEY, new Gson().toJson(wallet));
    }

    public PlayerWalletBO get(long pid) {
        String data = map(pid).get(BALANCE_KEY);
        return data == null ? null : new Gson().fromJson(data, PlayerWalletBO.class);
    }

    private RedisMap map(long pid) {
        return ContainerMgr.get().getRedis().getMap(KEY_PREFIX + pid);
    }
}
