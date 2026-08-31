package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

/**
 * 亲友圈所有合伙人统计
 *
 * @author Administrator
 */
@Data
public class ClubPartnerAloneCountBO implements Serializable {
    /**
     * 统计合伙人大赢家数
     */
    private int winnerPartner;
    /**
     * 统计亲友圈大赢家数
     */
    private int sumWinner;
    /**
     * 统计合伙人总局数
     */
    private int setPartner;
    /***
     * 统计亲友圈总局数
     */
    private int sumSet;
    /**
     * 统计合伙人成员总人数
     */
    private int sumPartner;
    /**
     * 统计亲友圈总人数
     */
    private int sumPlayer;
    /**
     * 合伙人PID
     */
    private long partnerPid = -1;

    public ClubPartnerAloneCountBO() {
        winnerPartner = 0;
        sumWinner = 0;
        setPartner = 0;
        sumSet = 0;
        sumPartner = 0;
        sumPlayer = 0;
    }

    public static String getItemsName(long partnerPid) {
        return "sum(IF(partnerPid = " + partnerPid + ",winner,0)) as winnerPartner," + "sum(winner) as sumWinner,"
                + "sum(IF(partnerPid = " + partnerPid + ",setCount,0)) as setPartner," + "sum(setCount) as `sumSet`";
    }

}
