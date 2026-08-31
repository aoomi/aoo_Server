package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

/**
 * 亲友圈所有合伙人统计
 *
 * @author Administrator
 */
@Data
public class ClubPartnerCountBO implements Serializable {
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
    /**
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

    public ClubPartnerCountBO() {
        winnerPartner = 0;
        sumWinner = 0;
        setPartner = 0;
        sumSet = 0;
        sumPartner = 0;
        sumPlayer = 0;
    }

    public static String getItemsName() {
        return "sum(IF(partnerPid > 0,winner,0)) as winnerPartner," + "sum(winner) as sumWinner,"
                + "sum(IF(partnerPid > 0,setCount,0)) as setPartner," + "sum(setCount) as sumSet";
    }

}
