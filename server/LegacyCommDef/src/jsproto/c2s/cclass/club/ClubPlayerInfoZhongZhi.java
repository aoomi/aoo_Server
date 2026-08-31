package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

import java.io.Serializable;

/**
 * 俱乐部玩家信息
 *
 * @author
 */
@Data
public class ClubPlayerInfoZhongZhi implements Serializable {
    /**
     * 玩家信息
     */
    private ShortPlayer shortPlayer;

    /**
     * 是否是管理,不是为null,是为1，2为创建者
     */
    private int minister;
    /**
     * 时间
     */
    private int time;
    /**
     * 竞技点
     */
    private double sportsPoint;

    private double eliminatePoint = 0d;
    private double alivePoint = 0d;

    public ClubPlayerInfoZhongZhi(ShortPlayer shortPlayer, int minister, int time, double sportsPoint, double eliminatePoint, double alivePoint) {
        this.shortPlayer = shortPlayer;
        this.sportsPoint = sportsPoint;
        this.minister = minister;
        this.time = time;
        this.eliminatePoint = eliminatePoint;
        this.alivePoint = alivePoint;
    }
}
