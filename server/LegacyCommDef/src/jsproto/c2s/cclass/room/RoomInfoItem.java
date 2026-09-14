package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间信息
 *
 * @param <T>
 * @author
 */
@Data
public class RoomInfoItem<T> implements Comparable<RoomInfoItem>, Serializable {
    /**
     * ID
     */
    private long id;
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
    private Integer gameId;
    /**
     * 总局数
     */
    private Integer setCount;
    /**
     * 人数
     */
    private Integer playerNum;
    /**
     * 玩家信息
     */
    private List<RoomPosInfoShort> posList = new ArrayList<>();
    /**
     * 房间创建时间
     */
    private Integer createTime;
    /**
     * 局数
     */
    private int setId;
    /**
     * 是否关闭
     */
    private boolean isClose;

    /**
     * 排序值（0:空配置、未满人房间,1:房间满过人,2:游戏中）
     */
    private int sort;

    /**
     * 房间配置
     */
    private Object roomCfg;

    /**
     * 配置Id
     */
    private int tagId;

    /**
     * 房间Id
     */
    private long roomId;

    /**
     * 密码
     */
    private String password;


    /**
     * 房间竞技点门槛
     */
    private Double roomSportsThreshold;

    /**
     * 0：//已开-人满-有人-空桌
     *
     * @param o
     * @return
     */

    @Override
    public int compareTo(RoomInfoItem o) {
        return this.getValue() - o.getValue();
    }

    /**
     * 1：//空桌固定序号-人满-已开（默认）
     *
     * @param o
     * @return
     */

    public int compareTo1(RoomInfoItem o) {

        return this.getValue1() - o.getValue1();
    }

    /**
     * 2://排序 有人-人满-已开-空桌
     *
     * @param o
     * @return
     */

    public int compareTo2(RoomInfoItem o) {
        return this.getValue2() - o.getValue2();
    }

    /***
     * 3: //有人-空桌-人满-已开
     * @param o
     * @return
     */

    public int compareTo3(RoomInfoItem o) {
        return this.getValue3() - o.getValue3();
    }

    /**
     * //已开-人满-有人-空桌
     *
     * @return
     */
    public int getValue() {
        switch (this.getSort()) {
            case 0:
                return 0;
            case 1:
                return 120;
            case 2:
                return 200 - this.getTagId();
            case 3:
                return 200 - this.getTagId();
            default:
                return 0;
        }
    }

    /**
     * //已开-人满-有人-空桌
     *
     * @return
     */
    public int getValue1() {
        switch (this.getSort()) {
            case 0:
                return 150;
            case 1:
                return 120;
            case 2:
                return this.getTagId();
            case 3:
                return this.getTagId();
            default:
                return 0;
        }
    }

    /**
     * 有人-人满-已开-空桌
     *
     * @return
     */
    public int getValue2() {
        switch (this.getSort()) {
            case 0:
                return 100 + this.getTagId();
            case 1:
                return 100 + this.getTagId();
            case 2:
                return this.getTagId();
            case 3:
                return 200 + this.getTagId();
            default:
                return 0;
        }
    }

    /**
     * //有人-空桌-人满-已开
     *
     * @return
     */
    public int getValue3() {
        switch (this.getSort()) {
            case 0:
                return 150 + this.getTagId();
            case 1:
                return 100 + this.getTagId();
            case 2:
                return 0;
            case 3:
                return this.getTagId();
            default:
                return 0;
        }
    }
}
