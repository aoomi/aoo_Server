package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 指定合伙人旗下玩家记录。
 *
 * @author Administrator
 */
@Data
public class ClubPartnerAloneRecordBO {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 参与局数
     */
    private int sumSet;
    /**
     * 大赢家次数
     */
    private int sumWinner;
    /**
     * 合伙人信息
     */
    private ShortPlayer player;

    public ClubPartnerAloneRecordBO() {
        super();
    }

    public ClubPartnerAloneRecordBO(int sumSet) {
        super();
        player = new ShortPlayer();
    }

    public static String getItemsName() {
        return "pid, " + "sum(setCount) as sumSet, " + "sum(winner) as sumWinner";
    }

}
