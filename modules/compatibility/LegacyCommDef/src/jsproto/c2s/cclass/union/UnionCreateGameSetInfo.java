package jsproto.c2s.cclass.union;


/**
 * 创建房间配置
 *
 * @author Administrator
 */
public class UnionCreateGameSetInfo<T> {
    /**
     * 配置Id
     */
    private long id;
    /**
     * 游戏配置
     */
    private T bRoomConfigure;
    /**
     * 游戏名称
     */
    private int gameId;
    /**
     * 当前设置状态
     */
    private int status = UnionDefine.UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL.value();


    public UnionCreateGameSetInfo(long id, T bRoomConfigure, int status, int gameId) {
        super();
        this.id = id;
        this.bRoomConfigure = bRoomConfigure;
        this.status = status;
        this.gameId = gameId;
    }
}
