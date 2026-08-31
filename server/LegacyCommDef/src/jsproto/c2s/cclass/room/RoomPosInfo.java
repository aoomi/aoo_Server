package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;

/**
 * 父类房间位置
 *
 * @author Administrator
 */
@Data
public class RoomPosInfo implements Serializable {
    // 位置
    private int pos;
    // PID
    private long pid;
    //accountID
    private long accountID;
    // 名称
    private String name;
    // 头像
    private String headImageUrl;
    // 性别
    private int sex;
    // 离线状态
    private boolean isLostConnect;
    // 是否显示离开
    private boolean isShowLeave;
    // 房间准备开始游戏
    private boolean roomReady;
    // 继续游戏准备
    private boolean gameReady;
    // 游戏托管状态
    private boolean trusteeship;
    //游戏旁观状态
    private boolean isPlaying;
    // 积分
    private int point;
    //金币
    private int gold;
    // 玩家IP
    private String playerIP;
    private Double sportsPoint;
    // 真是倍率的总增减积分
    private Double realPoint;
    private Long liuzuoSec;

    public RoomPosInfo() {
        super();
    }
}