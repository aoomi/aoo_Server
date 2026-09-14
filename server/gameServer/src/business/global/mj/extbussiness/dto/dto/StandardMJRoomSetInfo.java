package business.global.mj.extbussiness.dto;
				
import jsproto.c2s.cclass.mj.MJRoomSetInfo;

import java.util.List;

/**
 * 红中麻将当局信息				
 *				
 * @author Administrator				
 */				
public class StandardMJRoomSetInfo extends MJRoomSetInfo {

    public Integer jin1;
    public Integer jin2;
    public Integer jinJin;

    private List<StandardMJWaitingExInfo> biaoShiList;
    public String waitingExType;
    private int quanShu = 0;//圈数 -1 不显示

    public void setJin(int jin) {
        this.jin1 = jin;
    }

    public void setJinJin(Integer jinJin) {
        this.jinJin = jinJin;
    }

    public void setJin2(Integer jin2) {
        this.jin2 = jin2;
    }

    public void setBiaoShiList(List<StandardMJWaitingExInfo> biaoShiList) {
        this.biaoShiList = biaoShiList;
    }

    public void setWaitingExType(String waitingExType) {
        this.waitingExType = waitingExType;
    }

    public void setQuanShu(int quanShu) {
        this.quanShu = quanShu;
    }

}
