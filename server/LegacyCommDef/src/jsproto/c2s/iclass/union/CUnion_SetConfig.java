package jsproto.c2s.iclass.union;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class CUnion_SetConfig extends CUnion_Base {
    /**
     * 	赛事名称：描述性文字；
     */
    private String name = "";
    /**
     * 	加入申请：需要审核、不需要审核；
     */
    private int join;
    /**
     * 	退出申请：需要审核、不需要审核；
     */
    private int quit;
    /**
     * 	魔法表情：可以使用、不可以使用；
     */
    private int expression;
    /**
     * 	赛事状态：启用、停用；
     */
    private int state;
    /**
     *  竞技点清零：不清零、每天清零、每周清零、每月清零；
     */
    private int sports;
    /**
     * 裁判力度
     */
    private double initSports;
    /**
     * 比赛频率（30天，7天，每天）
     */
    private int matchRate;
    /**
     * 赛事淘汰
     */
    private double outSports;
    /**
     * 消耗类型(1-金币,2-房卡)
     */
    private int prizeType = 1;
    /**
     * 排名前50名
     */
    private int ranking;
    /**
     * 数量
     */
    private int value;
    /**
     * 显示的桌子数量
     */
    private int tableNum;

    /**
     * 允许亲友圈添加同赛事玩家 0:允许,1:不允许
     */
    private int joinClubSameUnion;


    public String getName() {
        if (StringUtils.isEmpty(this.name)) {
            return "";
        }
        return name;
    }
}
