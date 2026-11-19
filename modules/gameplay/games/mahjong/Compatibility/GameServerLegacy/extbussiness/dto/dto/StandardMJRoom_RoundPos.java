package business.global.mj.extbussiness.dto;
				
import jsproto.c2s.cclass.mj.BaseMJRoom_RoundPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 麻将回合信息
 *
 * @author Administrator
 * @date 2021/08/09
 */
public class StandardMJRoom_RoundPos extends BaseMJRoom_RoundPos {
    private List<List<Integer>> jieGangList = new ArrayList<>();
    private List<List<Integer>> buGangList = new ArrayList<>();
    private List<List<Integer>> anGangList = new ArrayList<>();

    /**
     * 听牌信息
     */
    private List<StandardMJTingInfo> tingInfoList=null;
    /**
     * 换三张的牌
     */
    protected List<Integer> firstChangeCardList = new ArrayList<>();//预先选的要换的张数

    public void setAnGangList(List<List<Integer>> anGangList) {
        this.anGangList = anGangList;
    }

    public void setJieGangList(List<List<Integer>> jieGangList) {
        this.jieGangList = jieGangList;
    }

    public void setBuGangList(List<List<Integer>> buGangList) {
        this.buGangList = buGangList;
    }

    public void setFirstChangeCardList(List<Integer> firstChangeCardList) {
        this.firstChangeCardList = firstChangeCardList;
    }

    public void setTingInfoList(List<StandardMJTingInfo> tingInfoList) {
        this.tingInfoList = tingInfoList;
    }
}
