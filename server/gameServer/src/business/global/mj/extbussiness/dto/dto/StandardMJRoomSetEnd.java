package business.global.mj.extbussiness.dto;

import jsproto.c2s.cclass.mj.BaseMJRoom_SetEnd;

import java.util.List;

/**
 * 红中麻将房间结算
 *
 * @author Administrator
 */
public class StandardMJRoomSetEnd extends BaseMJRoom_SetEnd {

    public Integer jin1;
    public Integer jin2;
    public Integer jinJin;

    public List<Integer> maList = null;
    public List<Integer> zhongList = null;
    public Boolean zhongMa;//客户端要端 抓马动画

    public void setJin(int jin) {
        this.jin1 = jin;
    }

    public void setJinJin(Integer jinJin) {
        this.jinJin = jinJin;
    }

    public void setJin2(Integer jin2) {
        this.jin2 = jin2;
    }

    public void setMaList(List<Integer> maList) {
        this.maList = maList;
    }

    public void setZhongList(List<Integer> zhongList) {
        this.zhongList = zhongList;
    }

    public void setZhongMa(Boolean zhongMa) {
        this.zhongMa = zhongMa;
    }
}
