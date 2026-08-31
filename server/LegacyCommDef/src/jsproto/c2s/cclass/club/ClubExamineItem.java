package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubExamineItem implements Serializable {
    /**
     * 日期
     */
    private String dateTime;
    /**
     * 玩家id
     */
    private long pid;
    /**
     * 玩家亲友圈身份id
     */
    private long toClubMemberId;
    /**
     * 执行审核玩家亲友圈身份id
     */
    private long doClubMemberId;
    /**
     * 审核前的值
     */
    private double beforeValue;
    /**
     * 审核的值
     */
    private double value;
    /**
     * 审核后的值
     */
    private double curValue;

    public static String getItemsName() {
        return "date_time as dateTime,toClubMemberId as toClubMemberId,doClubMemberId as doClubMemberId,pid as pid,beforeValue as beforeValue,value as value,curValue as curValue";
    }
}
