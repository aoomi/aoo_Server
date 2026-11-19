package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;

import java.io.Serializable;

/**
 * 玩家房间单独记录表
 *
 * @author Administrator
 */
@Data
public class ClubPlayerRoomAloneLogBOZhongZhi implements Serializable {
    /**
     * 序数id
     * 客户端排序用
     */
    private int id;
    /**
     * 玩家Pid
     */
    private long pid;

    /**
     * 玩家信息
     */
    private ShortPlayer player;
    /**
     * 查询出来的值
     */
    private double itemsValue;


    public static String getItemsNameZhongZhi(int type) {
        switch (UnionDefine.UNION_ZHONGZHI_RANKED_ITEM.valueOf(type)) {
            case ROOM_NUM:
                return "pid, "
                        + "count(pid) as itemsValue";
            case SET_NUM:
                return "pid, "
                        + "sum(setCount) as itemsValue";
            case WIN_LOSE_POINT:
                return "pid, "
                        + "sum(sportsPoint) as itemsValue";
            case BIG_WINER:
                return "pid, "
                        + "sum(winner) as itemsValue";
            case MAX_WIN_LOSE_POINT:
                return "pid, "
                        + "MAX(point) as itemsValue";
        }
        return "pid";
    }


}
