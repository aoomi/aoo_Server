package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
@Data
public class UnionAwardTimeAndNumInfo extends BaseSendMsg {
    /**
     * 颁奖记录
     */
    private List<UnionAwardTimeAndNum> awardTimeAndNums = new ArrayList<>();

    public UnionAwardTimeAndNumInfo(List<UnionAwardTimeAndNum> awardTimeAndNums) {
        this.awardTimeAndNums = awardTimeAndNums;
    }

    public UnionAwardTimeAndNumInfo() {
    }
}
