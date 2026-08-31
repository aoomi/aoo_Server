package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.Map;

public class SBase_RoomInvitation<T> extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private Long clubId;
    /**
     * 赛事Id
     */
    private Long unionId;
    /**
     * 房间号
     */
    private String roomKey;
    /**
     * 游戏id
     */
    private int gameId;
    /**
     * 玩家名称
     */
    private String playerName;
    /**
     * 名称
     */
    private String name;
    /**
     * 房间配置
     */
    private Map<String, Object> baseCreateRoom;

    public SBase_RoomInvitation(Long clubId, Long unionId, String roomKey, int gameId, String name, String playerName, Map<String, Object> baseCreateRoom) {
        this.clubId = clubId;
        this.unionId = unionId;
        this.roomKey = roomKey;
        this.gameId = gameId;
        this.name = name;
        this.playerName = playerName;
        this.baseCreateRoom = baseCreateRoom;
    }

    public static <T> SBase_RoomInvitation make(Long clubId, Long unionId, String roomKey, int gameId, String name, String playerName, Map<String, Object> baseCreateRoom) {
        return new SBase_RoomInvitation(clubId, unionId, roomKey, gameId, name, playerName, baseCreateRoom);
    }

}
