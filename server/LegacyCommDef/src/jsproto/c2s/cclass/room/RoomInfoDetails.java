package jsproto.c2s.cclass.room;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间详情
 */
@Data
public class RoomInfoDetails {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 名称
     */
    private String name;
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 类型Id
     */
    private int gameId;
    /**
     * 人数
     */
    private int playerNum;
    /**
     * 房间配置
     */
    private Object roomCfg;
    /**
     * 玩家信息
     */
    private List<RoomPosInfoShort> posList = new ArrayList<>();
    /**
     * 房间创建时间
     */
    private int createTime;

    /**
     * 总局数
     */
    private int setCount;
    /**
     * 局数
     */
    private int setId;

    /**
     * 房间状态类型
     */
    private Integer roomStateId;

    /**
     * 是否管理员、创造者 0:不是,1:是
     */
    private Integer isManage;

    /**
     * 标识Id
     */
    private int tagId;


}
