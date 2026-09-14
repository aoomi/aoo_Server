package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联盟设置
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
     * 	联盟名称：描述性文字；
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
     * 	联盟状态：启用、停用；
     */
    private int state;
    /**
     *  竞技点清零：不清零、每天清零、每周清零、每月清零；
     */
    private int sports;

    /**
     * 联盟总分
     */
    private long unionTotalScore;
    /**
     * 比赛频率（30天，7天，每天）
     */
    private int matchRate;
    /**
     * 联盟淘汰
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
     * 联盟管理钻石提醒
     */
    private int unionDiamondsAttentionMinister;
    /**
     * 联盟全员钻石提醒
     */
    private int unionDiamondsAttentionAll;
    /**
     * 联盟全员钻石提醒
     */
    private int tableNum;

    /**
     * 允许俱乐部添加同联盟成员 0:允许,1:不允许
     */
    private int joinClubSameUnion;

    public UnionSetConfig(long unionId, String name, int join, int quit, int expression, int state, int sports,
                          long unionTotalScore, int matchRate, double outSports, int prizeType, int ranking, int value, int unionDiamondsAttentionMinister, int unionDiamondsAttentionAll, int tableNum, int joinClubSameUnion) {
        this.unionId = unionId;
        this.name = name;
        this.join = join;
        this.quit = quit;
        this.expression = expression;
        this.state = state;
        this.sports = sports;
        this.unionTotalScore = unionTotalScore;
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
