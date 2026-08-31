package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionMemberItem {
    /**
     * 序数id
     * 客户端排序用
     */
    private long id;
    /**
     * 亲友圈名称
     */
    private String clubName;

    /**
     * 亲友圈id
     */
    private long clubId;

    /***
     * 创建者名称
     */
    private String createName;

    /**
     * 圈主id
     */
    private long createId;

    /**
     * 亲友圈key
     */
    private int clubSign;

    /**
     * 竞技点分数
     */
    private double sportsPoint;

    /**
     * 分数百分比
     */
    private double scorePercent;

    /**
     * 分数
     */
    private double scorePoint;

    /**
     * 人数
     */
    private int number;

    /**
     * 赛事创建者
     * UNION_CREATE(3),
     * 赛事管理员
     * UNION_MANAGE(2),
     * 赛事亲友圈创造者
     * UNION_CLUB(1),
     * 赛事普通成员
     * UNION_GENERAL(0)
     */
    private int unionPostType;

    /**
     * 总比赛分
     */
    private double sumSportsPoint;
    /**
     * 分成百分比
     */
    private double shareValue;
    /**
     * 分成固定值
     */
    private double shareFixedValue;
    /**
     * 分成类型
     */
    private int shareType;
    /**
     * 生存积分
     */
    private Double alivePoint;
    /**
     * 最终积分(中至用)
     */
    private double zhongZhiTotalPoint;
    /**
     * 淘汰分总和(整个圈每个玩家淘汰分总负值)
     */
    private double zhongZhiEliminatePointSum;
    /**
     * 大赢家
     */
    private int bigWinner;
    /**
     * 房卡消耗
     */
    private int consumeValue;
    /**
     * 活跃积分
     */
    private double promotionShareValue;
    /**
     * 成员总积分和
     */
    private double unionAllMemberPointTotal;

    public UnionMemberItem(String clubName, long clubId, String createName, long createId, int clubSign, double sportsPoint, double scorePercent, double scorePoint,
                           int number, int unionPostType, double sumSportsPoint) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.scorePercent = scorePercent;
        this.scorePoint = scorePoint;
        this.number = number;
        this.unionPostType = unionPostType;
        this.sumSportsPoint = sumSportsPoint;
    }

    public UnionMemberItem(String clubName, long clubId, String createName, long createId, int clubSign, double sportsPoint, double scorePercent, double scorePoint,
                           int number, int unionPostType, double sumSportsPoint, double shareValue, double shareFixedValue, int shareType, Double alivePoint, double zhongZhiTotalPoint, double zhongZhiEliminatePointSum) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.scorePercent = scorePercent;
        this.scorePoint = scorePoint;
        this.number = number;
        this.unionPostType = unionPostType;
        this.sumSportsPoint = sumSportsPoint;
        this.shareValue = shareValue;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
        this.alivePoint = alivePoint;
        this.zhongZhiTotalPoint = zhongZhiTotalPoint;
        this.zhongZhiEliminatePointSum = zhongZhiEliminatePointSum;
    }

    public UnionMemberItem(String clubName, long clubId, String createName, long createId, int clubSign, double sportsPoint, double scorePercent, double scorePoint,
                           int number, int unionPostType, double sumSportsPoint, double shareValue, double shareFixedValue, int shareType, Double alivePoint, double zhongZhiTotalPoint, double zhongZhiEliminatePointSum, double unionAllMemberPointTotal) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.scorePercent = scorePercent;
        this.scorePoint = scorePoint;
        this.number = number;
        this.unionPostType = unionPostType;
        this.sumSportsPoint = sumSportsPoint;
        this.shareValue = shareValue;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
        this.alivePoint = alivePoint;
        this.zhongZhiTotalPoint = zhongZhiTotalPoint;
        this.zhongZhiEliminatePointSum = zhongZhiEliminatePointSum;
        this.unionAllMemberPointTotal = unionAllMemberPointTotal;
    }

    public UnionMemberItem(String clubName, long clubId, String createName, long createId, int clubSign, double sportsPoint, double scorePercent, double scorePoint,
                           int number, int unionPostType, double sumSportsPoint, double shareValue, double shareFixedValue, int shareType, Double alivePoint, double zhongZhiTotalPoint, double zhongZhiEliminatePointSum, double unionAllMemberPointTotal, int consumeValue) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.scorePercent = scorePercent;
        this.scorePoint = scorePoint;
        this.number = number;
        this.unionPostType = unionPostType;
        this.sumSportsPoint = sumSportsPoint;
        this.shareValue = shareValue;
        this.shareFixedValue = shareFixedValue;
        this.shareType = shareType;
        this.alivePoint = alivePoint;
        this.zhongZhiTotalPoint = zhongZhiTotalPoint;
        this.zhongZhiEliminatePointSum = zhongZhiEliminatePointSum;
        this.unionAllMemberPointTotal = unionAllMemberPointTotal;
        this.consumeValue = consumeValue;
    }
}
