package cenum.mj;

public enum OpType {
    Not(0), // 空操作
    Hu(1), // 胡
    Peng(2), // 碰
    Gang(3), // 补杠
    JieGang(4), // 接杠
    AnGang(5), // 暗杠
    Chi(6), // 吃
    Out(7), // 出牌
    Pass(8), // 过
    QiangGangHu(9), // 抢杠胡
    BuHua(10), // 补花
    DanYou(11), // 单游
    ShuangYou(12), // 双游
    SanYou(13), // 三游
    QiangJin(14), // 抢金
    SanJinDao(15), // 三金倒
    SQPass(16), // 过去
    SiJinDao(17), // 四金
    WuJinDao(18), // 五金
    LiuJinDao(19), // 六金
    See(20), // 明查
    AnSee(21), // 暗查
    ShiSanYao(22), // 十三幺
    TingYouJin(23), // 听游金
    DDHu(24), // 对对胡
    TianHu(25), // 天胡
    PingHu(26), // 平胡
    Ting(27), // 听
    JinQue(28), // 金雀
    JinLong(29), // 金龙
    YiZhangHua(30), // 一张花
    WHuaWGang(31), // 无花无杠
    HunYiSe(32), // 混一色
    QingYiSe(33), // 清一色
    GuangYou(34), // 明游
    JinGang(38), // 金杠
    DiHu(39), // 地胡
    XDDHu(40), // 小对对胡
    DaDuiPeng(41), // 大对碰
    DanDiao(42), // 单吊
    HHDDHu(43), // 豪华对对胡
    DaSanYuan(44), // 大三元
    QYS_DDHu(47), // 清一色对对胡
    QYS_HHDDHu(48), // 清一色豪华对对胡
    SanJinYou(51), // 三金游
    LuanFeng(53), // 乱风
    CS_Tou(54), // 财神头
    SC_Ke(55), // 三财一刻
    SSBK(56), // 十三不靠
    SSBK_Qing(57), // 十三不靠清
    Check_QYS(58), // 检查-清一色
    MenZi(59), // 门子
    ChiLiang(60), // 吃亮
    GSKH(61), // 杠上开花

    BuQiuR(62), // 不求人 胡牌
    NiuPai(63), // 扭牌
    KouTing(65), // 扣听
    HaiDiLao(66), // 海底捞
    QingLong(67), // 青龙
    SanAnKe(68), // 三暗刻
    JiangYiSe(69), // 将一色
    QYS_PPHu(70),
    TWOHHDDHu(71), // 双豪华对对胡  双豪华七对
    THREEHHDDHu(72), // 三豪华对对胡 三豪华七对
    ZiYiSe(74), // 字一色
    JiePao(75), // 接炮
    KouPai(76), // 扣牌
    BBuHua(77), // 不补花
    MingLou(78), // 明搂
    DaTou(79), // 打骰
    SiBao(80), // 四宝
    Mo(81), // 摸
    BuMo(82), // 不摸
    QuanFeng(83), // 全风
    BaHua(84), // 八花
    MenQing(85), // 门清
    Wan(86), // 万
    Tiao(87), // 条
    Tong(88), // 筒
    TianGang(89), // 天杠
    SanHeiFeng(90), // 3黑风
    SiHeiFeng(91), // 4黑风
    PengDao(92), // 一碰砸到
    TianTing(93), // 天听
    BaoGang(94), // 爆杠
    Fan(95), // 翻
    Tou(96), // 偷
    GuiTou(97), // 鬼偷
    Zhao(98), // 招
    BaoJiao(99), // 爆叫
    ZQJB(100), // ZHENQIJIABA
    GangChong(101), // 刚冲
    BuNiu(102), // 不扭
    MingGang(103), // 明杠
    // 八一字牌
    WeiPai(104), // 偎牌
    SaoPai(105), // 扫牌
    TiPai(106), // 提牌----类似杠
    JieTiPai(107), // 接提牌-----类似接杠 扫穿动画
    BiPai(108), // 比牌-----自动吃牌
    ChouWei(109), // 臭偎牌-----自动吃牌
    SelfFan(110), // 自己翻牌-----配合客户端时间 中间插入一个回合
    BiPaiOp(111), // 比牌动作-----配合客户端时间 中间插入一个回合
    TiPaiKaiDuo(113), // 提牌开舵 提的那三张是碰的
    WuDangHu(112), // 无当胡
    Yao(114), // 要 要不要这张牌
    JiaHu(115), // 假胡 有些地方可能需要额外加分
    BuKouTing(116), Bu(117), // 补牌动作
    // npgzmj
    ZhaMaYaZhu(118), // 扎码押注
    BuZha(119), // 不扎
    // 安岳长牌
    BiTou(120), // 必须偷
    BiBao(121), // 必须报
    BaoQiDian(124), // 报七点
    NotBaoQiDian(125), // 不报七点
    MoPai(122), // 摸牌检测所有玩家能否吃碰杠胡
    JieGangBu(123), // 接杠补
    BuGangBu(127), // 补杠补
    LiangPaiDa(126),
    BiPeng(128),//必须碰
    Piao_Fen(129), //票分
    Gu_Chou(130), //箍臭
    Not_Gu(131), //不箍
    //红拐弯
    Pao(132), // 跑
    WeiJiu(133), // 偎旧
    ZiMo(134), // 自摸
    FistTiPai(135),//首提
    WaitDPosAnTi(136),//等待庄家暗提
    WaitDPosHu(137),//等待检测天胡
    WaitXPosAnTi(138),//等待闲家暗提
    WaitMoPai(139),//等待摸牌
    WaitOut(140),//等待出牌
    TiPaiWaitMoPai(141),//闲家提牌等待摸牌
    MingBai(142),//明摆
    BaoDing(143),//报定
    GuanMen(144),//关门
    JingDiao(145), // 精吊
    BaoTing(146),//报听
    KanPai(147),//坎牌 林楠要求
    FeiTing(148), // 飞听
    FeiTingJinZiMo(149), // 飞听仅自摸
    YaoGang(150), //
    YaoGangHu(151), // 遥杠胡
    SuiJing(152), // 随精
    TeShuDan(153),//特殊蛋
    DuiBao(154),//对宝
    MoBao(155),//摸宝
    GuoDan(156),//过蛋
    MoQianTing(157),//摸牌前听
    HuanSanZhang(158),//换三张
    LiangPai(159),//亮牌
    LiangPai_Not(160),//不亮牌
    ShuaiYao(161), // 甩幺
    TuoGuang(162), // 脱光
    WaitDPosAnWei(163),//等待庄家暗偎
    TiWei(164),//起手提偎
    PengLaiZi(165),//碰癞子
    BaPai(166),//巴牌
    BoZiMo(171),//博自摸
    BaoJ(172), // 报叫
    GSP(173), // 杠上炮
    ShuaiZhang(174),//甩张
    XiaoSa(175),//潇洒 阜新麻将报听用
    YingBaDui(176),//硬八对
    //宁波麻将
    ChengBao(177),//承包
    FangQi(178),//放弃
    GenDa(179),//跟打
    JiaTing(180),//夹听
    MaiSanZhang(181),//买三张
    BuBaoJ(182),//不报叫
    QiangFengHu(185),//强风胡
    BaoTingNoDo(186),//报听不需要塞牌
    JiaTingNoDo(187),//夹听不需要赛拍
    Agree(188),//同意
    DisAgree(189),//反对
    Liang(190),//亮

    XuanLaiZi(191),//选癞子
    Ying(192),//硬
    QingHu(193), // 请胡
    ZhuaQingHu(194), // 抓请胡
    DingQue(195), // 定缺
    HaiDiPao(196),//海底炮
    Tan(197), // 摊
    Shao(198), // 韶
    AutoTan(199), //自动摊
    CiHu(200),//次胡
    GuoSao(201),// 过扫
    SaoChuan(202), //扫穿
    KaiZhao(203),//开招
    DiaoDui(204),//钓对
    JiaZhiGang(205),//假直杠
    ShuaiPai(206),//甩牌
    BuShuai(207),//不甩
    PengLiang(208),//碰亮
    AnGangBu(209),//暗杠补
    GangBu(210),//暗杠补
    LiangJia(211),//亮夹
    BaoJia(212),//报夹
    SiYou(213),//四游
    BaYou(214),//八游
    ShiLiuYou(215),//十六游
    SanShiErYou(216),//三十二游
    DiXiaHuPai(217),//地下胡牌
    ChiTing(218), // 吃听
    PengTing(219), // 碰听
    ChiTingOut(220), // 吃听出牌
    PengTingOut(221), // 碰听出牌
    KaiGangBaoTing(222), // 开杠报听
    WaitDPosZhaoPai(223),//等待庄家招牌
    WaitXPosZhaoPai(224),//等待闲家招牌
    MoPaiOut(225),//摸的牌没人操作自动打出
    TiePai(226),//贴牌
    ZouPai(227),//走牌
    ZhaoAnPai(228),//招牌暗
    TiePaiChi(229),//贴牌吃了一张牌的吃牌
    ZhaoMingPai(230),//招牌明
    ZhaoPaiChi(231),//招牌吃--->5张变6张
    BaoTai(232), // 包台
    BuBao(233), // 不包
    XuanCaiShen(234), //选财神
    GanTa(235), //赶踏
    FanPai(236), //fanpai
    TongShangDing(237), //fanpai
    QuXiao(238),
    RuanPeng(239), // 软碰
    RuanGang(240), // 软补杠
    TiaoPai(241), // 挑牌
    RuanJieGang(242), // 软接杠
    RuanAnGang(243), // 软暗杠
    Piao(244), // 漂
    //菏泽麻将专属
    BuPao(245),//不跑
    Zha(247),//炸
    BuQue(248),//不缺
    QueMen(249),//缺门
    La(250),//拉庄
    BuLa(246),//不拉
    ZFB(247),//不拉
    QiangTiHu(251),//抢提胡
    QiPai(252),//起牌
    FaPai(253),//发牌
    QiFei(254),//五喜起飞
    LiangYi(255),//两仪
    SiXiang(256),//四象
    BaGua(257),//八卦
    MoDiPai(258),//摸底牌
    MoLiangPai(259),//摸亮牌
    BaiPai(260),//摆牌
    JiaZhuang(261), // 加庄
    BuJia(262), // 不加
    BaoLiuPeng(263), // 爆六碰
    BuBaoPai(264), // 不爆牌
    NotShow(265), // 不显示  demo定缺用
    ;

    private int value;

    private OpType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static OpType valueOf(int value) {
        for (OpType flow : OpType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return OpType.Out;
    }

};
