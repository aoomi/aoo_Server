package jsproto.c2s.cclass.mj.template;

import cenum.mj.HuType;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class MJTemplateSet_Pos extends BaseMJSet_Pos {

    private boolean isTing;
    protected List<Integer> changeCardList = null;//选中的三张
    private OpType dingQue = null;//定缺
    private HuType huType = null;//胡牌类型 目前只有血战才有用到，用于图标
    private HuType huOpType = null;//胡牌类型 目前只有血流/血战才有用到，用于动画

    private List<MJTemplateHuInfo> huInfos = null;//血流模式胡牌流水
    private Map<Integer, Integer> huInfo = null;//听牌分信息
    /**
     * 飘分
     */
    public Integer piaoFen = null;
    /**
     * 跑
     */
    public Integer pao = null;
    public Integer piao = null;
    public Integer mai = null;
    public Integer bao = null;
    //自动胡牌
    private Integer autoHu = null;
    //自动打牌
    private Integer autoOut = null;

    public void addChangeCardList(int card) {
        changeCardList.add(card);
    }
}
