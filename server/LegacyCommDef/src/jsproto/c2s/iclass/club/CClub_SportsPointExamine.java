package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 执行竞技点更新
 *
 * @author zaf
 */
@Data
public class CClub_SportsPointExamine extends CClub_SportsPointUpdate {

    /**
     * 日期类型
     */
    private int dateType;
    /**
     * 执行亲友圈id
     */
    private long exeClubId;

    public CClub_SportsPointExamine(long clubId, long opPid, int type, double value, int dateType, long exeClubId) {
        super(clubId, opPid, type, value);
        this.dateType = dateType;
        this.exeClubId = exeClubId;
    }

    @Override
    public String toString() {
        return "CClub_SportsPointExamine{" +
                super.toString() +
                "dateType=" + dateType +
                ", exeClubId=" + exeClubId +
                '}';
    }
}