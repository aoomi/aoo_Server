package business.global.mj.extbussiness.dto;
				
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;

import java.util.List;
import java.util.Map;

/**
 * 红中麻将 配置				
 *				
 * @author Clark				
 */				
// 一局中各位置的信息										
public class StandardMJSet_Pos extends BaseMJSet_Pos {
    private boolean isTing;
    private Map<Integer, Integer> huInfo = null;//听牌分信息
    /**
     * 跑
     */
    public Integer pao = null;
    public Integer piao = null;
    public Integer mai = null;
    public Integer bao = null;

    protected List<Integer> changeCardList = null;//选中的三张

    private List<Integer> huaList;

    public void setHuaList(List<Integer> huaList) {
        this.huaList = huaList;
    }

    public void setTing(boolean ting) {
        this.isTing = ting;
    }

    public void setPiao(Integer piao) {
        this.piao = piao;
    }

    public void setChangeCardList(List<Integer> changeCardList) {
        this.changeCardList = changeCardList;
    }

    public void addChangeCardList(int card) {
        changeCardList.add(card);
    }

    public void setHuInfo(Map<Integer, Integer> huInfo) {
        this.huInfo = huInfo;
    }
}
