package business.cdxzmj.c2s.cclass;


import cenum.mj.HuType;
import jsproto.c2s.cclass.room.AbsBaseResults;


/**
 * 总结算
 */
public class CDXZMJResults extends AbsBaseResults {
    private int zhongMaPoint;//中马次数	
    private int anGangPoint;//暗杠次数	
    private int mingGangPoint;//明杠次数	
    private boolean DianPaoWang = false;//点炮王	
    private boolean winner = false;//大赢家	

    public void addMingGangPoint(int point) {
        this.mingGangPoint += point;
    }

    public void addZhongMaPoint(int point) {
        this.zhongMaPoint += point;
    }

    public void addAnGangPoint(int point) {
        this.anGangPoint += point;
    }

    public void setDianPaoWang(boolean dianPaoWang) {
        DianPaoWang = dianPaoWang;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public void addJiePaoPoint(HuType hType) {
        if (hType.name().contains("Hu") && !hType.name().contains("NotHu")) {
            super.addJiePaoPoint(HuType.JiePao);
        }
    }

    public void addZimoPoint(HuType hType) {
        if (hType.name().contains("ZiMo")) {
            super.addZimoPoint(HuType.ZiMo);

        }
    }

}						
