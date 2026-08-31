package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

import java.io.Serializable;

/**
 * 玩家房间单独记录表
 *
 * @author Administrator
 */
@Data
public class ClubPlayerRoomAloneLogBO implements Serializable {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 大赢家次数
     */
    private int winner;
    /**
     * 房卡数
     */
    private int roomCardSize;
    /**
     * 房卡
     */
    private int roomCard;
    /**
     * 分数
     */
    private int point;
    /**
     * 参与次数
     */
    private int size;
    /**
     * 玩家信息
     */
    private ShortPlayer player;

    /**
     * 竞技点
     */
    private double sportsPoint;

    /**
     * 页数
     */
    private int pageNumTotal;

    public ClubPlayerRoomAloneLogBO() {
        pid = 0L;
        winner = 0;
        roomCardSize = 0;
        roomCard = 0;
        point = 0;
        size = 0;
        sportsPoint = 0;
        player = null;
        pageNumTotal = 0;
    }

    public static String getItemsName() {
        return "pid, "
                + "sum(winner) as winner,"
                + "sum(`value`) as roomCard,"
                + "sum(point) as point,"
                + "count(pid) as size,"
                + "sum(sportsPoint) as sportsPoint";
    }

}
