package core.config.server;

import com.ddm.server.common.CommLogD;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefGameType;
import jsproto.c2s.cclass.GameType;

import java.util.Objects;

/**
 * 游戏类型管理配置
 *
 * @author Administrator
 */
public class GameTypeMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static GameTypeMgr instance = new GameTypeMgr();
    }

    // 私有化构造方法
    private GameTypeMgr() {
    }

    // 获取单例
    public static GameTypeMgr getInstance() {
        return SingletonHolder.instance;
    }

    // 头部
    private final static String HANDLER = "core.network.client2game.handler.%s";
    // 创建配置类
    private final static String CREATE_CONFIG = "business.%s.c2s.iclass.C%s_CreateRoom";

    /**
     * 获取游戏类型
     *
     * @param id 游戏ID
     * @return
     */
    public GameType gameType(long id) {
        RefGameType refGameType = RefDataMgr.get(RefGameType.class, id);
        return Objects.isNull(refGameType) ? null : new GameType((int) refGameType.getId(), refGameType.getName(), refGameType.getType());
    }

    /**
     * 获取游戏类型
     *
     * @param name 游戏ID
     * @return
     */
    public GameType gameType(String name) {
        return RefDataMgr.getAll(RefGameType.class).values().stream().filter(type -> type.getName().equalsIgnoreCase(name)).map(k -> null == k ? null : new GameType((int) k.getId(), k.getName(), k.getType())).findFirst().orElse(null);
    }

    /**
     * 获取头部
     */
    public String Handler(long id) {
        RefGameType gameType = RefDataMgr.get(RefGameType.class, id);
        // 检查游戏类型是否存在。
        if (Objects.isNull(gameType)) {
            CommLogD.error("Handler null == gameType ID:{}", id);
            return "";
        }
        return String.format(HANDLER, gameType.getName()).toLowerCase();
    }


    public String CreateRoomConfig(long id) {
        RefGameType gameType = RefDataMgr.get(RefGameType.class, id);
        // 检查游戏类型是否存在。
        if (Objects.isNull(gameType)) {
            CommLogD.error("Handler null == gameType ID:{}", id);
            return "";
        }
        return String.format(CREATE_CONFIG, gameType.getName().toLowerCase(), gameType.getName().toUpperCase());
    }

    /**
     * 获取头部
     */
    public String Handler(String name) {
        RefGameType gameType = RefDataMgr.getAll(RefGameType.class).values().stream()
                .filter((x) -> name.equals(x.getName()))
                .findAny()
                .orElse(null);
        // 检查游戏类型是否存在。
        if (Objects.isNull(gameType)) {
            CommLogD.error("GameStart null == gameType Name:{}", name);
            return "";
        }
        return String.format(HANDLER, gameType.getName()).toLowerCase();
    }
}
