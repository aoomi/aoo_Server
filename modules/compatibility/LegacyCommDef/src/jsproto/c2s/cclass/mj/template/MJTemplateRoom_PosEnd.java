package jsproto.c2s.cclass.mj.template;

import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class MJTemplateRoom_PosEnd extends BaseMJRoom_PosEnd {
    /**
     * 是否吊精
     */
    public Map<?, Integer> huTypeMap = null;
    public int posId = 0; // 位置
    /**
     * 飘分
     */
    public Integer piaoFen = null;
    public List<Integer> zhongList = null;
    public List<Integer> maList = null;
    public Map<Integer, List<Integer>> gangMap;//杠 键是杠的cardtype 值是玩家posid
    public OpType dingQue = null;//定缺
    public List<Integer> winList = null;//胡的类型下面显示数字
    public Integer piao;
    public Integer pao;
    public Integer bao;
    public Integer mai;
    private List<? extends MJTemplate_XueLiuPlayerLiuSui> xueLiuPlayerLiuShuiInfos = null;

}
