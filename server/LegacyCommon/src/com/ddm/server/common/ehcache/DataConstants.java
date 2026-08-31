package com.ddm.server.common.ehcache;

public class DataConstants {
    /**
     * 赛事分成比缓存key
     */
    public static final String SCORE_PERCENT_CACHE = "unionId%dclubId%dconfigId%d";

    /**
     * 亲友圈分成比缓存key
     */
    public static final String PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT = "LEVELpid%dunionId%dclubId%dconfigId%d";

    /**
     * 亲友圈战绩数据统计缓存key
     */
    public static final String CLUB_TOTAL_PAGE_INFO = "TOTALCLUBPAGEID%dTYPE%d";
    /**
     * 亲友圈战绩数据统计缓存key
     */
    public static final String CLUB_TOTAL_INFO = "TOTALCLUBID%dTYPE%d";
    /**
     * 赛事战绩数据统计缓存key
     */
    public static final String UNION_TOTAL_INFO = "TOTALUNIONID%dTYPE%d";
    /**
     * 查询指定亲友圈所有合伙人的统计数据缓存key
     */
    public static final String CLUB_PARTNER_ALONE_COUNT = "PARTNERALONECLUBID%dTYPE%dPID%d";

    /**
     * 查询指定亲友圈所有合伙人的统计数据缓存key
     */
    public static final String CLUB_PARTNER_ALL_COUNT = "PARTNERALLCLUBID%dTYPE%d";

    /**
     * 亲友圈合伙人个人战绩统计缓存key
     */
    public static final String CLUB_PARTNER_PERSONAL_COUNT = "PARTNERPERSONALCLUBID%dPID%dPPID%dTYPE%d";


    /**
     * 亲友圈合伙人个人战绩统计缓存key
     */
    public static final String CLUB_PROMOTION_PERSONAL_COUNT = "PROMOTIONPERSONALCLUBID%dPID%dPPID%d";

    /**
     * 抽奖人群
     */
    public static final String LUCK_DRAW_ASSIGN_CROWD = "LUCKDRAWPID%d%s";

    /**
     * 赛事人数统计
     */
    public static final String UNION_ONLINE_COUNT = "UNIONONLINECOUNT%d";

    /**
     * 亲友圈人数统计
     */
    public static final String CLUB_ONLINE_COUNT = "CLUBONLINECOUNT%d";

    /**
     * 近一周亲友圈竞技分成
     */
    public static final String SCORE_SPORT_WEEK = "unionId%dclubId%d";

    /**
     * 赛事经营统计
     */
    public static final String UNION_ROOM_CONFIG_PRIZE_POOL_COUNT = "URCPPCUID%dTIME%sNAME%s";
    /**
     * 某个玩家的所有下线竞技点之和
     */
    public static final String SPORTS_POINT_ALL_WARNING = "clubId%dclubMemberId%d";
    /**
     * 某个亲友圈的所有下线竞技点之和
     */
    public static final String SPORTS_POINT_CLUB_ALL_WARNING = "clubId%dunionId%d";

    /**
     * 中至赛事经营统计
     */
    public static final String UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI = "UNIONCOUNTUNIONID%dTIME%sNAME%s";

    /**
     * 中至赛事经营统计
     * 根据日期的数据
     */
    public static final String UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE = "UNIONCOUNTUNIONID%dTIME%sNAME%sTYPE%d";
    /**
     * 中至赛事经营统计
     * 根据日期的数据
     * 七天的统计
     */
    public static final String UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE_SEVENDAY = "SEVENDAYUNIONCOUNTUNIONID%dTYPE%d";

    /**
     * 中至赛事经营统计
     * 根据日期的数据
     * 七天的统计
     */
    public static final String UNION_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TYPE_RECENT = "RECENTUNIONCOUNTUNIONID%dTYPE%d";
    /**
     * 中至赛事经营统计 亲友圈层面 最终积分
     */
    public static final String CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI = "CLUBCOUNTUNIONID%dCLUBID%d";
    /**
     * 中至赛事经营统计 亲友圈层面 成员总积分和
     */
    public static final String CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ALL_ZHONGZHI = "CLUBCOUNTUNIONID%dCLUBID%d";
    /**
     * 中至赛事经营统计 亲友圈层面
     * 最近六天
     */
    public static final String CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_RECENT = "CLUBCOUNTRECENTUNIONID%dCLUBID%d";
    /**
     * 中至赛事经营统计 亲友圈层面
     * 根据日期的数据
     */
    public static final String CLUB_ROOM_CONFIG_PRIZE_POOL_COUNT_ZHONGZHI_TIME = "CLUBCOUNTUNIONID%dCLUBID%dTIME%s";
    /**
     * 中至赛事经营统计
     * 亲友圈一天的数据缓存
     */
    public static final String CLUB_TODYA_COUNT_ZHONGZHI = "CLUBTODAYCLUBID%d";
    /**
     * 中至赛事经营统计
     * 玩家一天的数据缓存(包括底下的所属)
     */
    public static final String PLAYER_TODYA_COUNT_ZHONGZHI = "PLAYERTODAYPID%dCLUBID%d";

}
