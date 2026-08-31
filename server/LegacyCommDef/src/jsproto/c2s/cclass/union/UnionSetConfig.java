package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛事设置
 *
 * @author
 */
@Data
@NoArgsConstructor
public class UnionSetConfig {
    /**
     * 联赛ID
     */
    private long unionId;
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
     * 赛事管理员钻石提醒
     */
    private int unionDiamondsAttentionMinister;
    /**
     * 赛事全员钻石提醒
     */
    private int unionDiamondsAttentionAll;
    /**
     * 赛事全员钻石提醒
     */
    private int tableNum;

    /**
     * 允许亲友圈添加同赛事玩家 0:允许,1:不允许
     */
    private int joinClubSameUnion;

    public UnionSetConfig(long unionId, String name, int join, int quit, int expression, int state, int sports,
                          double initSports, int matchRate, double outSports, int prizeType, int ranking, int value, int unionDiamondsAttentionMinister, int unionDiamondsAttentionAll, int tableNum, int joinClubSameUnion) {
        this.unionId = unionId;
        this.name = name;
        this.join = join;
        this.quit = quit;
        this.expression = expression;
        this.state = state;
        this.sports = sports;
        this.initSports = initSports;
        this.matchRate = matchRate;
        this.outSports = outSports;
        this.prizeType = prizeType;
        this.ranking = ranking;
        this.value = value;
        this.unionDiamondsAttentionMinister = unionDiamondsAttentionMinister;
        this.unionDiamondsAttentionAll = unionDiamondsAttentionAll;
        this.tableNum = tableNum;
        this.joinClubSameUnion = joinClubSameUnion;
    }
}
