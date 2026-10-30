package jsproto.c2s.cclass.mj.template;

import jsproto.c2s.cclass.mj.BaseMJRoom_RoundPos;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class MJTemplateRoom_RoundPos extends BaseMJRoom_RoundPos {
    /**
     * 能杠的牌
     */
    protected List<Integer> firstChangeCardList = new ArrayList<>();//预先选的要换的张数
    /**
     * 暗杠二维
     */
    protected List<List<Integer>> anGangList = new ArrayList<>();
    /**
     * 补杠二维
     */
    protected List<List<Integer>> buGangList = new ArrayList<>();
    /**
     * 接杠二维
     */
    protected List<List<Integer>> jieGangList = new ArrayList<>();
    /**
     * 摇杠二维
     */
    protected List<List<Integer>> yaoGangList = new ArrayList<>();
    /**
     * 听牌信息
     */
    private List<MJTemplateTingInfo> tingInfoList = null;
}
