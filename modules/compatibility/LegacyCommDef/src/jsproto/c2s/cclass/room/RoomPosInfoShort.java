package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;

/**
 * 父类房间位置
 *
 * @author Administrator
 */
@Data
public class RoomPosInfoShort implements Serializable {
    // 位置
    private int pos;
    // PID
    private long pid;
    // 名称
    private String name;
    // 头像
    private String headImageUrl;
    // 离线状态
    private boolean isLostConnect;
    // 房间准备开始游戏
    private boolean roomReady;
    //俱乐部名称
    private String clubName;
    //上级玩家名称
    private String upLevelName;
    //俱乐部id
    private long clubID;

    public RoomPosInfoShort() {
        super();
    }
}