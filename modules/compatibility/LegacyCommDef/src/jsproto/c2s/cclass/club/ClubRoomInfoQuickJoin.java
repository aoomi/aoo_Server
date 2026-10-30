package jsproto.c2s.cclass.club;

/**
 * @author FengZhnag
 * @date 2022/9/20 15:48
 * @description 快速加入房间
 */
public class ClubRoomInfoQuickJoin {

    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 类型Id
     */
    private Integer gameId;

    public ClubRoomInfoQuickJoin(String roomKey, Integer gameId) {
        this.roomKey = roomKey;
        this.gameId = gameId;
    }
}
