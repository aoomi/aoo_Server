package business.scjymj.c2s.cclass;

import jsproto.c2s.cclass.room.AbsBaseResults;

public class SCJYMJResults extends AbsBaseResults {
    // 自摸次数
    private int ziMoCount;
    // 接炮次数
    private int jiePaoCount;
    // 点炮次数
    private int diaoPaoCount;
    // 暗杠次数
    private int anGangCount;
    // 明杠次数
    private int mingGangCount;
    // 坐庄次数
    private int zhuangCount;
    // 赢分次数
    private int winPointCount;

    public void setZiMoCount(int ziMoCount) {
        this.ziMoCount += ziMoCount;
    }

    public void setJiePaoCount(int jiePaoCount) {
        this.jiePaoCount += jiePaoCount;
    }

    public void setDiaoPaoCount(int diaoPaoCount) {
        this.diaoPaoCount += diaoPaoCount;
    }

    public void setAnGangCount(int anGangCount) {
        this.anGangCount += anGangCount;
    }

    public void setMingGangCount(int mingGangCount) {
        this.mingGangCount += mingGangCount;
    }

    public void setZhuangCount(boolean zhuangCount) {
        if (zhuangCount) {
            this.zhuangCount += 1;
        }
    }

    public void setWinPointCount(boolean winPointCount) {
        if (winPointCount) {
            this.winPointCount += 1;
        }
    }
}
