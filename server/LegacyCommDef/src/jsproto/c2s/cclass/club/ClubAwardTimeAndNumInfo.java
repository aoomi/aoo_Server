package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.union.UnionAwardTimeAndNum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
@Data
public class ClubAwardTimeAndNumInfo extends BaseSendMsg {
    /**
     * 颁奖记录
     */
    private List<ClubAwardTimeAndNum> awardTimeAndNums = new ArrayList<>();

    public ClubAwardTimeAndNumInfo(List<ClubAwardTimeAndNum> awardTimeAndNums) {
        this.awardTimeAndNums = awardTimeAndNums;
    }

    public ClubAwardTimeAndNumInfo() {
    }
}
