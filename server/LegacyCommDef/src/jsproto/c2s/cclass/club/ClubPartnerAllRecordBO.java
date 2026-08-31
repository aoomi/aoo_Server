package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 指定亲友圈的所有合伙人记录
 *
 * @author Administrator
 */
@Data
public class ClubPartnerAllRecordBO {
    /**
     * 合伙人PID
     */
    private long partnerPid;
    /***
     * 成员参与局数
     */
    private int setPartner;
    /**
     * 全员大赢家总数
     */
    private int winnerPartner;
    /**
     * 玩家数
     */
    private int sumPartner;
    /**
     * 合伙人信息
     */
    private ShortPlayer player;

    public static String getItemsName() {
        return "partnerPid,sum(IF(partnerPid > 0,setCount,0)) as setPartner,sum(winner) as winnerPartner";
    }

    public void setSumPartner(Long sumPartner) {
        if (null == sumPartner) {
            this.sumPartner = 0;
        } else {
            this.sumPartner = sumPartner.intValue();
        }
    }

    @Override
    public String toString() {
        return "ClubPartnerAllRecordBO [partnerPid=" + partnerPid + ", setPartner=" + setPartner + ", winnerPartner="
                + winnerPartner + ", sumPartner=" + sumPartner + ", player=" + player + "]";
    }

}
