package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;

/**
 * 房间信息
 *
 * @param <T>
 * @author * 简化版 2021/11/1傅哥要求
 */
@Data
public class RoomInfoItemShortOne<T> implements Comparable<RoomInfoItemShortOne>, Serializable {

    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 类型Id
     */
    private Integer gameId;

    /**
     * 人数
     */
    private Integer playerNum;


    /**
     * 局数
     */
    private int setId;


    /**
     * 排序值（0:空配置、未满人房间,1:房间满过人,2:游戏中）
     */
    private int sort;


    /**
     * 配置Id
     */
    private int tagId;

    /**
     * 竞技点倍数
     */
    private Double sportsDouble = 1D;

    /**
     * 0：//已开-人满-有人-空桌
     *
     * @param o
     * @return
     */

    @Override
    public int compareTo(RoomInfoItemShortOne o) {
        return this.getValue() - o.getValue();
    }

    /**
     * 1：//空桌固定序号-人满-已开（默认）
     *
     * @param o
     * @return
     */

    public int compareTo1(RoomInfoItemShortOne o) {

        return this.getValue1() - o.getValue1();
    }

    /**
     * 2://排序 有人-人满-已开-空桌
     *
     * @param o
     * @return
     */

    public int compareTo2(RoomInfoItemShortOne o) {
        return this.getValue2() - o.getValue2();
    }

    /***
     * 3: //有人-空桌-人满-已开
     * @param o
     * @return
     */

    public int compareTo3(RoomInfoItemShortOne o) {
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
