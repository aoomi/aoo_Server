package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.club.Club_define.Club_CreateGameSetStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * 亲友圈创建游戏配置信息
 *
 * @param <T>
 */
@Data
public class ClubCreateGameSetInfo<T> implements Serializable {
    /**
     * 游戏配置
     */
    private T bRoomConfigure;
    /**
     * 游戏名称
     */
    private String gameType;
    /**
     * 当前设置状态
     */
    private int status = Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value();
    /**
     * 创建的房间数
     */
    private int roomCount = 0;
    /**
     * 房间创建时间
     */
    private int createTime = 0;


    public ClubCreateGameSetInfo(T bRoomConfigure, int status, int roomCount, int createTime, String gameType) {
        super();
        this.bRoomConfigure = bRoomConfigure;
        this.status = status;
        this.roomCount = roomCount;
        this.createTime = createTime;
        this.gameType = gameType;
    }
}
