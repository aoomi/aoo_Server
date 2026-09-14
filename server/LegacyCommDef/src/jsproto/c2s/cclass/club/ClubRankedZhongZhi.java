package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author FengZhnag
 * @date 2022/6/10 14:59
 * @description 中至排行榜信息
 */
@Data
public class ClubRankedZhongZhi {
    /**
     * 数组列表
     */
    private List<ClubPlayerRoomAloneLogBOZhongZhi> recordList = new ArrayList<>();
    /**
     * 底下自己信息
     */
    private ClubRankedZhongZhiSelf clubRankedZhongZhiSelf;
    /**
     * 当前选择的type
     * 0参与房间数 1参与小局数 2积分 3大赢家 4比赛最高分
     */
    private int type;

    public static ClubRankedZhongZhi make(List<ClubPlayerRoomAloneLogBOZhongZhi> recordList, ClubRankedZhongZhiSelf clubRankedZhongZhiSelf) {
        ClubRankedZhongZhi ret = new ClubRankedZhongZhi();
        ret.setRecordList(recordList);
        ret.setClubRankedZhongZhiSelf(clubRankedZhongZhiSelf);
        return ret;
    }
}
