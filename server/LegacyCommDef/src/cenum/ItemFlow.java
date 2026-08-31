package cenum;

public enum ItemFlow {
    // 未知类型
    Unkwon(0),
    //	clearRoomCard
    // 清空公共钻石
    CLEAR_PUBLIC_ROOM_CARD(1),
    // 公共钻石转换过来的
    PUBLIC_CARD_CHANGE(2),
    // 测试账号
    GM_TEST(3),
    // 房卡房间
    RoomCardRoom(1000),
    // 房卡-亲友圈房间
    RoomCardClubRoom(2000),
    // 圈卡-亲友圈房间
    ClubCardClubRoom(2001),
    // 圈卡-充值
    ClubCardRecharge(2002),
    // 管理员给玩家圈卡操作
    FamilyClubCard(2003),
    // 会长对玩家操作圈卡
    FamilyClubCardToPlayer(2004),
    // 首次加入亲友圈奖励圈卡
    ClubFirstRewardClubCard(2005),
    // 房卡-赛事房间
    RoomCardUnionRoom(2006),
    // 会长给玩家钻石
    FamilyCardToPlayer(2007),
    // 后台充值、撤回操作
    PHPToPlayer(2008),
    // 切换玩家指定城市的钻石
    CHANGE_PALYER_CITY_ROOM_CARD(2009),
    // 创建亲友圈
    CLUB_CREATE(2010),
    // 其他类型
    // 洗牌
    XiPai(3000),
    // 签到奖励
    SignIn(3001),
    // 玩家充值获得
    Recharge(3002),
    // 手机号绑定奖励
    Phone(3003),
    // 邮件奖励
    Mail(3004),
    // 发送表情
    SendGift(3005),
    // 绑定会长送房卡
    FamilyID(3006),
    // 会长送房卡
    AgentGiveRoomCard(3007),
    // 后台直充
    ZleRecharge(3008),
    // 房卡兑换乐豆
    CardRoomExchangeGold(3009),
    // 比赛场报名费
    Arena(3010),
    // 比赛场奖励
    ArenaReward(3011),
    // 日常任务
    TaskActiveGain(3012),
    // 场地费
    SiteFee(3013),
    // 练习场房间
    GoldRoom(3014),
    // 抽奖
    LuckDraw(3015),

    // 推广奖励
    RefererReward(9001),
    // 赛事竞技点改变
    UNION_SPORTS_POINT_CHANGE(4001),
    // 赛事后台竞技点改变
    UNION_BACKSTAGE_SPORTS_POINT_CHANGE(4002),
    // 亲友圈竞技点改变
    CLUB_SPORTS_POINT_CHANGE(4003),


    // 赛事奖励
    UNION_MATCH_REWARD(4004),
    // 联赛初始裁判力度
    UNION_INIT_SPORTS(4005),
    // 联赛清空裁判力度
    UNION_CLEAR_SPORTS(4006),
    // 联赛清空比赛分申请退赛
    UNION_BACK_OFF(4007),
    // 联赛清空比赛分操作申请退赛
    UNION_BACK_OFF_OP(4008),
    // 联赛申请退赛-回退
    UNION_BACK_OFF_ROLLBACK(4009),
    // 获得排名的奖励
    UNION_RANKING_MATCH_REWARD(4010),
    //	ApplyRematch
    // 联赛复赛操作
    UNION_APPLY_REMATCH(4011),
    // 联赛清空比赛分操作申请退赛
    UNION_APPLY_REMATCH_OP(4012),
    // 联赛复赛操作-回退
    UNION_APPLY_REMATCH_ROLLBACK(4013),
    // 推广员 操作比赛分
    PROMOTION_LEVEL_SPORTS_POINT_CHANGE(4014),
    // 切牌消耗
    QIEPAI_CONSUME_POINT_CHANGE(4015),
    //保险箱分数增加
    UNION_CASE_SPORT_ADD(4016),
    //保险箱分数减少
    UNION_CASE_SPORT_SUB(4017),
    // 亲友圈竞技点审核
    CLUB_SPORTS_POINT_EXAMINE(4018),


    // 疲劳值系统
    // 竞技点游戏中消耗
    SPORTS_POINT_GAME(4020),
    // 竞技点房费消耗
    SPORTS_POINT_ROOM(4021),
    // 竞技点收益利润
    SPORTS_POINT_PROFIT(4022),

    // 疲劳值增加
    FATIGUEd_VALUE_PLUS(4023),
    // 疲劳值值减少
    FATIGUEd_VALUE_REDUCE(4024),
    // 疲劳值清零
    FATIGUEd_VALUE_CLEAR(4025),
    // 推广员竞技点分成收益利润
    PROMOTION_SPORTS_POINT_PROFIT(4026),
    // 推广员竞技点分成收益利润到保险箱
    PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT(4027),

    // 亲友圈推广员-创建者操作
    CLUB_PROMOTION_ACTIVE_CREATE(5001),
    // 亲友圈推广员-房间获得
    CLUB_PROMOTION_ACTIVE_ROOM(5002),
    // 亲友圈推广员-清空
    CLUB_PROMOTION_ACTIVE_CLEAR(5003),
    // 亲友圈成员移除
    CLUB_MEMBER_REMOVE(5004),

    // 亲友圈竞技点改变--踢出保险箱有分数的人
    CLUB_CASE_SPORTS_POINT_TICHU(5005),


//	// 亲友圈成员（状态、管理员、合伙人）
//	Club_PLAYER_WEIPIZHUN(4001), //未批准
//	Club_PLAYER_JUJIE(4002), //已拒绝
//	Club_PLAYER_JIARU(4003), //为已加入
//	Club_PLAYER_TICHU(4004), //为已踢出
//	Club_PLAYER_YAOQING(4005), //已邀请
//	Club_PLAYER_JUJIEYAOQING(4006), //邀请被拒
//	Club_PLAYER_TUICHU(4007), //已退出
//	Club_PLAYER_TICHU_CLOSE(4008), //关闭俱乐部踢出
//	Club_PLAYER_BECOME_MGR(4009), //成为管理员
//	Club_PLAYER_CANCEL_MGR(4010), //取消管理员
//	Club_PLAYER_BECOME_BAN(4011), //禁止游戏
//	Club_PLAYER_CANCEL_BAN(4012), //取消禁止游戏
//	Club_PLAYER_APPOINT_PARTNER(4013), //亲友圈任命合伙人
//	Club_PLAYER_CANCEL_PARTNER(4014), //亲友圈取消合伙人
//	Club_PLAYER_DELETE_PARTNER(4015), //亲友圈删除合伙人
//	Club_PLAYER_CHANGE_PARTNER(4016), //亲友圈变更从属合伙人
//	Club_PLAYER_PARTNER_MEMBER_ADD(4017), //亲友圈合伙人添加成员
    ;

    private int value;

    private ItemFlow(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ItemFlow itemFlow(int value) {
        for (ItemFlow flow : ItemFlow.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ItemFlow.Unkwon;
    }
}
