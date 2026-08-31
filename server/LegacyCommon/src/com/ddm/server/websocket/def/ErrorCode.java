package com.ddm.server.websocket.def;

public enum ErrorCode {
    Not(-1),// 空
    Success(0), // 消息成功发送
    Server_Maintain(10), // 服务端维护
    Banned_Login(11), //禁止登陆
    Game_Maintain(12), // 游戏维护
    // 基础错误
    Unknown(100), // 消息成功发送
    InvalidParam(101), // 参数错误
    ErrorSysMsg(102), // 错误回报系统提示码
    NotAllow(103), // 非法操作
    NotSetRound(104), // 非当前回合操作
    REPEAT_SUBMIT(106),//出现重复提交
    CanChangeSeat(107),//不能交换位置
    // 常见错误
    Request_IncorrectSession(200), // session类型不匹配
    Request_NullResponse(201), // 没有对应response
    Request_UnknownMesaggeType(202), // 未知消息类型
    Request_RequestTimeout(203), // 请求超时
    Request_ServerNotConnected(204), // 服务器未连接
    Request_NotFoundHandler(205), // 未找到协议处理器
    Request_OutcomeUnknown(206), // 请求可能已受理，禁止自动重试并按业务键查询
    Request_DependencyUnavailable(207), // 依赖明确未受理，可稍后重试

    // 房间类错误
    Room_Key_ISNULL(300), //房间key为空
    Room_NOT_Find(301), //房间未找到
    Room_BaseConfigure_ISNULL(302), //房间配置为空
    Room_GameType_ISNULL(303), //房间游戏类型为空
    Room_STATUS_ERROR(304), // 房间状态错误


    ROOM_SPORTS_POINT_NOT_ENOUGH(305),// 房间竞技点不足玩家通知
    ROOM_SPORTS_POINT_ENOUGH(306),// 房间竞技点足够玩家通知

    ROOM_GAME_SERVER_CHANGE(307),// 房间服务器变化通知
    ROOM_ONE_PLAYER_NOT_SELECT_POS(308),// 一个人的时候不能切换位置

    // 未归类部分
    NotFound_Achievement(411), // 未找到已完成的成就
    Object_IsNull(412),// 对象是空


    // 主副本相关
    Dungeon_WinCD(601), // 副本CD中
    Dungeon_NotBegin(602), // 主副本未开始

    // 未归类部分
    NotEnough_CompleteCount(626), // 抽奖次数不足
    NotEnough_CompleteLevel(627), // 完成阶段不足
    NotEnough_UnlockCond(641), // 解锁条件不足

    NotEnough_CheckGold(800), // 玩家金币不符合。
    NotEnough_CheckRoomCard(801), // 玩家房卡不符合。

    // 数量不足
    NotEnough_Currency(900), // 货币不足
    NotEnough_Crystal(901), // 钻石不足
    NotEnough_Gold(902), // 游戏币不足
    NotEnough_RoomCard(903), // 房卡不足
    NotEnough_GoldHigh(904), // 游戏币过高
    NotEnough_RoomCardByXiPai(905), // 房卡不足,洗牌失败，不用提示充值
    NotEnough_ClubCard(906), // 圈卡不足
    NotEnough_RoomCost_Error(907),//房卡配置错误
    NotEnough_ClubCard_Error(908), //圈卡设置错误
    NotEnough_Arena(909), // 比赛券不足
    NotEnough_SportsPoint(910),//竞技点不足够
    NotEnough_CityRoomCard(911),//城市货币不足够
    NotEnough_CaseSportsPoint(912),//保险箱竞技点不足够

    ALREADY_FETCH(1306), // 已经领取了该奖励

    // 活动相关
    Signin_AlreadySigned(1501), // 已经签到
    Activity_Close(1502), // 活动已经关闭
    Not_Enough(1503), // 条件未达成
    Already_Picked(1504), // 已经领取
    Receive_NumberNot(1505), // 领取次数上限
    Not_Send_Gift(1506),    // 禁止魔法表情

    // 世界BOSS
    WorldBoss_NotChallengeTimes(1803), // 挑战次数已用完
    WorldBoss_FightInCD(1804), // 处于CD状态
    WorldBoss_IsDeath(1806), // 世界boss已被消灭
    WorldBoss_NotOpen(1811), // 世界Boss未开启
    WorldBoss_InspireFull(1812), // 世界Boss未开启
    WorldBoss_NotEnoughRank(1813), // 世界Boss伤害排名不够

    // 副本
    Instance_Locked(3001), // 副本未解锁
    Instance_NotEounghTimes(3002), // 副本未解锁
    Instance_NotEounghMaterial(3003), // 材料不够
    Instance_Full(3004), // 副本已全部挑战完
    Instance_NotPassed(3004), // 副本已全部挑战完

    // 商店相关
    Store_RefreshFull(4001), // 刷新次数已满
    NotEnough_StoreRefreshCost(4002), // 刷新次数已满
    Goods_HasRefresh(4003), //
    Goods_Soldout(4004), //
    Goods_NotEnough(4005), //
    NotFound_RefGoodsInfo(4006), //
    Goods_PriceLess(4007), //

    // 流动房间

    NotFind_Player(5000), // 找不到房间
    NotFind_Room(5001), // 找不到房间
    NotFind_Pos(5002), // 找不到座位
    NotEnough_CreateRoom(5003), // 可以创建的房间数量不足
    NotEmpty_Pos(5004), // 座位非空
    NoPower_RoomOwnner(5005), // 没有权限
    NoPower_RoomJoinner(5006), // 没有权限
    NoPower_Pos(5007), // 没有权限
    NoPower_SetState(5008), //
    NoPower_SetPos(5009), //


    NotExist_CurSet(5010), // 不存在当前局
    NotExist_Ranked(5011), // 不存在牌序
    Exist_OtherRoom(5012), // 已在其他房间
    Player_PidError(5013), // 用户PID错误
    DissolveRoom_Already_Exists(5014), // 已经发起了房间解散

    PlayBack_Error(5015), PlayBack_NotExist(5016), PlayBack_Expire(5017),

    Card_Error(5018), ExitROOM_ERROR(5019), // 退出房间失败
    ExitROOM_ERROR_NOTFINDROOM(5020), // 退出房间失败 找不到房间
    CITY_ID_ERROR(5021),//城市id
    FAMILY_CITY_ID_ERROR(5022),//选择城市id错误
    ROOM_PASSWORD_ERROR(5023),//房间密码错误
    WarningSport_RoomJoinner(5024), // 预警值不够 加入房间失败
    ROOM_RESET_INFO(5025),//重新刷新房间信息
    PersonalWarningSport_RoomJoinner(5026), // 个人预警值不够 加入房间失败
    ChangeAllyLeader(5027), // 当前赛事正在更换赛事主裁判，暂停游戏，请稍后再试！预计半小时
    Exist_InRoom(5028), // 已在房间内

    NotExist_Family(5110), // 工会不存在
    EXIST_SAME_IP(5111),//存在相同的IP地址
    APART_LOCATION(5112),//相距位置出现问题
    Not_Family_Owner(5113),//不代理

    NoPower_CARD_NOTALLOW(5114), //还贡错误
    Create_Room_Error(5115),    //创建房间错误
    Error_Phone(5116), // 错误手机号
    Error_Old_Phone(5117), // 错误旧手机号
    Error_Code(5118),    //错误验证码
    Not_Exist_Phone(5119), // 不存在手机号
    Exist_Phone(5120), // 存在电话号
    Not_Family_Member(5121),    //不是代理成员
    DissolveRoom_Error(5125), // 解散次数上限
    POSITIONING_NOT_ON(5126),//定位未开启
    ZhaDanBuKeChai(5130), //炸弹不可拆
    FAMILY_POWER_ERROR(5131),// 代理权限不足

    PLAYER_NOT_ONLINE(5132),// 玩家不在线
    CLUB_SPORT_POINT_WARN(5133),// 您所在的亲友圈比赛分低于预警值，无法加入比赛，请联系管理
    CLUB_ALIVE_POINT_WARN(5134),// 您所在的亲友圈生存积分过低，无法加入比赛，请联系管理
    CLUB_PERSONAL_ALIVE_POINT_WARN(5136),// 您所在的推广员队伍或上级队伍比赛分低于生存积分，无法加入比赛，请联系管理，无法加入比赛，请联系管理

    MUST_BIND_PHONE(5137),// 请先绑定手机

    CLUB_MAXCRATESET(6001), // 俱乐部创建配置达到最大值
    CLUB_NOTMINISTER(6002), // 不是管理员
    CLUB_SETMINISTERMAXTATNTWO(6004), // 管理员超过两个
    CLUB_MEMBERCANNOTCREATEROOM(6005), //俱乐部成员不可以创建房间
    CLUB_GROUPING(6006), //分组限制

    CLUB_NOTCLUBMEMBER(6003), // 不是俱乐部会员
    CLUB_EXIST_CREATE(6007),//亲友圈存在创建者
    CLUB_NOT_EXIST(6008),//亲友圈不存在
    CLUB_NOT_EXIST_MEMBER_INFO(6009),//亲友圈不存在成员信息
    CLUB_NOT_EXIST_CREATE(6010),//亲友圈创建者不存在

    CLUB_PARTNER_UPPER_LIMIT(6011),//亲友圈合伙人上限
    CLUB_PARTNER_EXIST(6012),//该玩家已绑定合伙人
    CLUB_PARTNER_CREATE(6013),//亲友圈合伙人和创建者不允许变更。
    CLUB_NOT_PARTNER(6014),//不是亲友圈合伙人。。
    CLUB_NOT_CREATE(6015),//不是亲友圈创造者。
    CLUB_INVITATION_ERROR(6016),//亲友圈-邀请失败
    CLUB_BAN_GAME(6017),//成员被禁止游戏
    CLUB_ENTER_NOT_CLUBMEMBER(6018), //通过房间号进入房间时提示：不是亲友圈成员，返回亲友圈Key.
    CLUB_EXIST_JIARU(6019), //已加入亲友圈
    CLUB_PLAYER_WEIPIZHUN(6020),// 等待亲友圈管理员批准中
    CLUB_MEMBER_UPPER_LIMIT(6021),// 俱乐部人数已满
    CLUB_PLAYER_UPPER_LIMIT(6022),// 自己加入的俱乐部数达到上限
    CLUB_PLAYER_ADD(6028),    // 亲友圈人员添加失败
    CLUB_EXIST_UNION(6032),         /// 亲友圈已经加入赛事
    CLUB_QUIT_UNION(6033),         /// 亲友圈已经退出赛事
    CLUB_MEMBER_SAME_POST_TYPE(6034),//亲友圈成员相同权利
    CLUB_NOT_JOIN_UNION(6035),//亲友圈没加入赛事
    CLUB_UNION_CREATE(6036),//您的亲友圈为联盟盟主，当前不可解散
    CLUB_NAME_ERROR(6037),// 亲友圈名称错误
    CLUB_NAME_EXIST(6038),// 亲友圈名称存在重复
    CLUB_NOT_EXIST_GROUPING(6039),//不存在分组
    CLUB_GROUPING_ID_ERROR(6040),//分组id不存在
    CLUB_GROUPING_PID_EXIST(6041),//已在该组中
    CLUB_GROUPING_PID_NOT_EXIST(6042),//不在该组中

    CLUB_NOT_PROMOTION(6043),//不是推广员
    CLUB_EXIST_PROMOTION(6044),//推广员

    CLUB_EXIST_PROMOTION_SUBIRDINATE(6045),//还有推广员下属存在

    CLUB_NOT_SUBORDINATE(6045),//不是下属成员
    CLUB_EXIST_SUBORDINATE(6046),// 存在下属成员
    CLUB_EXIST_PROMOTION_TICHU_ERROR(6047),//删除推广员后，该玩家将被踢出亲友圈
    CLUB_UP_LEVEL_MEMBER_NOT_EXIST(6048),// 上级推广员不存在
    CLUB_MEMBER_ROOM_ERROR(6049),// 成员在游戏中无法踢出亲友圈
    CLUB_MEMBER_PROMOTION_BELONG(6050),// 修改归属的时候不能切换到原来的归属或者下线
    CLUB_MEMBER_PROMOTION_PERCENT_LESS(6051),// 修改归属的时候百分比不能更小
    CLUB_MEMBER_PROMOTION_CHANGE_IS_EXIT(6052),// 当前有人正在修改归属，请稍后再试

    CLUB_MEMBER_PROMOTION_LEVEL_SHARE_LOWER(6053),      //    当前分支下的下级推广员分成存在百分比形式，无法设置分成为固定值，请先修改
    CLUB_MEMBER_PROMOTION_LEVEL_SHARE_UP(6054),       // 上级推广员分成为固定值，无法设置为百分比
    CLUB_MEMBER_TIME_ERROR(6055),       // “每日0-2点时，系统数据统计中无法进行本操作”
    CLUB_NOT_INVITE(6056),//没有邀请权限
    CLUB_PROMOTION_SHOW_THAN_NINE(6057),//设置推广员的数据显示超过九项
    CLUB_EXAMINE(6058), // 改日期已经审核过
    CLUB_MEMBER_PROMOTION_SHARE_TYPE_DIFFERENT(6059),      //    修改失败,上级分成类型与当前分支类型不一致,请联系上级
    CLUB_MEMBER_PROMOTION_FIXED_LESS(6060),// 修改归属的时候固定值不能更小
    CLUB_CLUBMEMBER_EXIST_APPLOVE(6061), // 正在退出申请审核中
    CLUB_MEMBER_PROMOTION_PERCENT_IS_NOT_ALLOW_MULTI(6062),// 百分比不允许批量修改房间分成
    CLUB_MEMBER_PROMOTION_EXIST_LOWER(6063),// 该推广员还有下级人员,不能取消推广
    CLUB_MEMBER_PROMOTION_SECTION_EXIST(6064),// 联盟没有存在区间分成的数据
    CLUB_MEMBER_PROMOTION_SECTION_CHANGE_IS_EXIT(6065),// 当前有人正在修改区间分成，请稍后再试
    CLUB_MEMBER_CLUB_PROMOTION_CHANGE_IS_EXIT(6066),//整个亲友圈有人正在修改归属，请稍后再试

    CLUB_MEMBER_JOIN_TIME_LIMIT(6067),//退出加入同一个亲友圈添加10分钟时间限制
    CLUB_PROMOTION_CHANGE_NOT_CLUBCREATE(6068),//不是圈主不能移动到圈主名下

    UNION_NOT_EXIST(6100),  // 赛事不存在
    UNION_NOT_MANAGE(6101),  // 不是赛事管理员或创建者
    UNION_NOT_EXIST_MEMBER(6102),// 赛事成员信息不存在
    UNION_EXIST_ADD_OTHERS_UNION(6103),// 赛事已加入其它赛事605
    UNION_MEMBER_STATUS_ERROR(6104),// 赛事成员状态不符合
    UNION_JOIN_FAIL(6105),// 加入赛事失败
    UNION_PLAYER_JIARU(6106),//已加入赛事
    UNION_PLAYER_WEIPIZHUN(6107),// 已申请加入赛事
    UNION_MEMBER_UPPER_LIMIT(6108),// 赛事人数已满
    UNION_PLAYER_UPPER_LIMIT(6109),// 自己加入的赛事数达到上限
    UNION_PLAYER_YAOQING_FAIL(6110),// 赛事邀请失败
    UNION_NOT_EXIST_ROOM_CFG_ID(6111),// 赛事房间配置ID不存在
    UNION_ROOM_CFG_ERROR(6112),//赛事房间配置错误
    UNION_ID_ERROR(6113),// 赛事id错误
    UNION_MEMBER_SAME_POST_TYPE(6114),//赛事成员相同权利
    UNION_NOT_CREATE(6115),  // 赛事不是创建者
    UNION_BELOW_THRESHOLD_VALUE(6116),//低于门槛值
    UNION_BAN_GAME(6117),//成员被禁止游戏
    UNION_MAXCRATESET(6118), // 俱乐部创建配置达到最大值
    UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO(6119),//存在竞技点不等于0
    UNION_PLAYER_IN_GAME_ERROR(6120),// 赛事玩家游戏中无法扣除竞技点
    UNION_ENTER_NOT_CLUBMEMBER(6121), //该房间为联盟房间，请通过亲友圈加入.

    UNION_VALUE_ERROR(6122),//数量错误
    UNION_PRIZE_TYPE_ERROR(6123),// 奖励类型错误
    UNION_RANKING_ERROR(6124),// 排名错误
    UNION_INIT_SPORTS_ERROR(6125),//裁判力度错误
    UNION_OUT_SPORTS_ERROR(6126),//淘汰值错误
    UNION_EXIST_CLUB(6127),  // 赛事存在亲友圈
    UNION_EXIST_PLAYING_ROOM(6128),  // 赛事当前有房间正在进行，无法解散
    UNION_MATCH_STATE_ERROR(6129),// 赛事比赛状态错误
    UNION_NAME_ERROR(6130),// 赛事名称错误
    UNION_NAME_EXIST(6131),// 赛事名称存在重复
    UNION_BACK_OFF_PLAYING(6132),// 您已申请退赛，当前无法进行比赛，请取消退赛申请或联系赛事举办方
    UNION_APPLY_REMATCH_PLAYING(6133),//您的复赛申请等待审批中，请联系赛事举办方
    UNION_GROUPING(6134), //分组限制
    UNION_STATE_STOP(6135), //赛事状态停用

    UNION_NOT_EXIST_GROUPING(6136),//不存在分组
    UNION_GROUPING_ID_ERROR(6137),//分组id不存在
    UNION_GROUPING_PID_EXIST(6138),//已在该组中
    UNION_GROUPING_PID_NOT_EXIST(6139),//不在该组中

    UNION_CUR_MATCH_PLAYING(6140),//比赛进行中
    UNION_CUR_APPLY_REMATCH(6141),//复赛申请中
    UNION_CUR_BACK_OFF(6142),//退赛申请中
    PLAYER_IN_GAME_ERROR(6143),// 玩家游戏中无法操作
    UNION_NOT_OPERATE_YOURSELF(6144),// 不能自己操作自己
    UNION_JOIN_CLUB_SAME_UNION(6145),// 允许亲友圈添加同赛事玩家
    UNION_ROOM_NOT_UNION_ROOM(6146),//不是赛事房间
    UNION_NOT_OPEN_CASE_SPORT(6147),//没有开启保险箱功能
    UNION_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO(6148),//存在保险箱中比赛分不等于0
    UNION_EXIST_CASE_SPORTS_POINT(6149),//玩家保险箱中还有竞技点值，确认是否踢出玩家
    UNION_IS_CLOSE_CASE(6150),//当前正在关闭保险箱功能中，请稍后再试
    CLUB_EXIST_CASE_SPORTS_POINT_NOT_EQUAL_ZERO(6151),//亲友圈存在保险箱中比赛分不等于0
    UNION_EXAMINE_STATUS_AUTO(6152),// 当前为自动审核，不能进行手动审核
    UNION_SKIN_IS_CHANGED(6153),// 今日皮肤已经设定过
    UNION_POWER_ERROR(6154),// 赛事权限不足
    UNION_CLUB_NOT_SAME(6155),// 该玩家不属于您的亲友圈，无法查看
    UNION_CLUB_NOT_MY_PROMOTION(6156),// 该玩家不属于您的分支，无法查看
    UNION_TYPE_NOT_EXIST(6157),// 赛事类型不存在
    CLUB_PLAYER_EXIT_IN_OTHER_UNION(6300),// 同赛事不同亲友圈不能重复拉人
    ARENA_NOT_JOIN_ROOMCARD(7001), // 比赛场中，不能加入房卡场或金币场。
    ARENA_NOT_QUALIFICATION(7002), // 比赛场中，玩家不属于该指定俱乐部。
    ARENA_EXCLUSIVE_KEY_NOT(7003), // 比赛场中,找不到专属Key
    ARENA_ENROLL(7004), // 玩家已经报名
    ARENA_NOT_WAIT(7005), // 比赛场不在等待中
    ARENA_NOT_START(7006), // 比赛场未开始或已经结束
    ARENA_ENEOLL_LIMIT(7007), // 报名次数受限
    ARENA_END(7008), // 比赛场结束
    JNMJ_MINGLOU_ERROR(7009),//济宁麻将明搂错误
    AYPDK_HEITAO_THREE(7200), // 黑桃三必出
    CITY_NOT_FIND(7201),//城市信息未找到
    CITY_LIST_EMPTY(7202),//城市列表是空

    // 战绩类错误
    RECORD_NOT_EXIST(8000),// 第一页没有战绩

    // 请在中间添加,不可以超过10000,客户端判断使用
    PackNot_Action(10000), PackRun_Error(10001), KickOut_LoginError(10002), KickOut_CreateNewHeroError(10003), KickOut_NotCreateToken(10004), KickOut_OtherLogin(10005), KickOut_AccountNotFind(10006), KickOut_AccountPswError(10007), KickOut_AccountAuthorizationFail(10008), KickOut_TokenExpire(10009), Http_ServerNotStart(10010), Http_PackRunError(10011), Http_PackNotAction(10012), Http_NotFindPack(10013), Http_RequestAccountServerFail(10014), Http_RequestOrderServerFail(10015), KickOut_ServerClose(10016), KickOut_AccountLoginError(10017), KickOut_AccountTokenError(10018), KickOut_NotFreePortID(
            10019), PackRun_NotFindPack(10020), KickOut_ClientVersion(10021),
    Task_Error(11201),//错误任务
    Task_Reward(11202),//领取奖励失败
    Share_Error_Type(11401),//分享错误类型
    Share_Error_Reward(11402),//分享错误奖励
    Request_BeatError(10086),// 心跳异常
    BW_NotChaiZha(11501),//炸弹不能拆，拆了就没赏了
    Is_Trusteeship(15000), // 托管无法操作
    OP_CARD_ERROR(15001),// 操作牌错误
    CHANGE_CAED_ERROR(15002),//换牌错误
    ;

    private short value;

    private ErrorCode(int value) {
        this.value = (short) value;
    }

    public short value() {
        return this.value;
    }
}
