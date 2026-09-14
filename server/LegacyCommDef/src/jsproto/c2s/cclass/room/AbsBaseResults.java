package jsproto.c2s.cclass.room;

import cenum.mj.HuType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家房间结算记录
 *
 * @author Administrator
 */
@Data
public class AbsBaseResults extends BaseResults {
    // 胡次数
    private int huCnt = 0;
    // 胡类型列表
    private List<HuType> huTypes = new ArrayList<HuType>();
    // 点炮
    private int dianPaoPoint = 0;
    // 接炮
    private int jiePaoPoint = 0;
    // 自摸
    private int ziMoPoint = 0;

    public void addHuTypes(HuType hType) {
        this.huTypes.add(hType);
    }

    public void addDianPaoPoint(HuType hType) {
        if (HuType.DianPao.equals(hType)) {
            this.dianPaoPoint += 1;
        }
    }

    public void addJiePaoPoint(HuType hType) {
        if (HuType.JiePao.equals(hType)) {
            this.jiePaoPoint += 1;
        }
    }

    public void addZimoPoint(HuType hType) {
        if (HuType.ZiMo.equals(hType)) {
            this.ziMoPoint += 1;
        }
    }


}
