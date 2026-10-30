package cenum;

public enum PKOpType {
    /**
     * 空操作
     */
    Not(0),
    /**
     * 过
     */
    Pass(1),
    /**
     * 碰
     */
    Peng(2),
    /**
     * 吃
     */
    Chi(3),
    /**
     * 带
     */
    Dai(4),
    /**
     * 打牌
     */
    Out(5),
    /**
     * 第二次勾牌
     */
    MoGou(6),
    /**
     * 摸后打
     */
    MoOut(7),
    /**
     * 勾牌
     */
    Gou(8),
    /**
     * 重新开始
     */
    Reset(9),
    /**
     * 呼叫报警
     */
    Call(10),
    /**
     * 挑战动作
     */
    Challenge(11),
    /**
     * 不换牌挑战动作
     */
    NotChangeCardChallenge(12),
    /**
     * 放炸弹
     */
    Bomb(13),
    /**
     * 连环炸弹
     */
    SerialBomb(14),
    /**
     * 换牌
     */
    ChangeCard(15),
    /**
     * 不换牌
     */
    NotChangeCard(16),
    /**
     * 同意
     */
    Agree(17),
    /**
     * 拒绝
     */
    Refuse(18),
    /**
     * 过放炸弹
     */
    PassBomb(19),
    /**
     * 过连环炸弹
     */
    PassSerialBomb(20),
    /**
     * 抢挑(不换牌)
     */
    RobNotChangeCard(21),
    /**
     * 让牌
     */
    LET_GO(22),
    /**
     * //加注
     */
    ADD_BET(23),
    /**
     * //跟注
     */
    FALLOW_BET(24),
    /**
     * //筹码全部下注
     */
    ALL_IN(25),//筹码全部下注
    /**
     * //弃牌
     */
    PASS_CARD(26),//弃牌
    /**
     * 下注
     */
    BET(27),

    /**
     * 反主
     */
    FAN_ZHU(28),
    /**
     * 反无主
     */
    FAN_WU_ZHU(29),
    /**
     * 亮主一张
     */
    LIANG_ZHU1(30),
    /**
     * 亮主2张
     */
    LIANG_ZHU2(31),
    /**
     * 扣底牌
     */
    KOU_DI_PAI(32),
    /**
     * 报边
     */
    BaoBian(33),
    /**
     * 打独
     */
    DaDu(34),
    /**
     * 方块主
     */
    FANG_KUAI(35),
    /**
     * 梅花主
     */
    MEI_HUA(36),
    /**
     * 红桃主
     */
    HONG_TAO(37),
    /**
     * 黑桃主
     */
    HEI_TAO(38),
    /**
     * 无主NT
     */
    WU_ZHU_NT(39),
    /**
     * 挑选牌
     */
    XuanPai(40),
    /**
     * 过河
     */
    GUO_HE(41),
    /**
     * 不过河
     */
    BU_GUO_HE(42),
    /**
     * 加倍
     */
    JIA_BEI(43),
    /**
     * 不加倍
     */
    BU_JIA_BEI(44),
    /**
     * 自保
     */
    ZI_BAO(45),
    /**
     * 自反
     */
    ZI_FAN(46),

    /**
     * 被动过河
     */
    FOLLOW_GUO_HE(47),
    /**
     * 報分
     * 三打哈用
     */
    Bao_Fen(48),
    /**
     * 报副
     * 三打哈用
     */
    Bao_Fu(49),
    /**
     * 埋底
     */
    Mai_Di(50),
    /**
     * 无主
     */
    Wu_Zhu(51),
    /**
     * 我可以
     * 0是我有分 1我没分 2打得起 2打不起
     */
    I_CAN(52),
    /**
     * 投降
     */
    I_TOUXIANG(53),
    /**
     * 叫地主
     */
    JiaoDiZhu(54),
    /**
     * 不叫
     */
    BuJiao(55),
    /**
     * 明牌
     */
    MingPai(56),
    /**
     * 不明
     */
    BuMing(57),
    /**
     * 不打独
     */
    BuDaDu(58),
    /**
     * 比奖
     */
    BiJiang(59),
    /**
     * 奖牌
     */
    JiangPai(60),
    /**
     * 没奖
     */
    MeiJiang(61),

    /**
     * 投降
     */
    Surrender(62),
    /**
     * 摸牌
     */
    Mo(63),
    /**
     * 拾牌
     */
    Shi(64),
    /**
     * 翻牌
     */
    Fan(65),
    /**
     * 弃牌
     */
    Abandon(66),
    /**
     * 牌局结束
     */
    Finish(67),
    /**
     * 提交
     */
    Declare(68),
    /**
     * 牌局结束  通过放置槽
     */
    FinishSlot(69),
    /**
     * 换三张
     */
    ChangeThree(70),
    /**
     * 叫牌
     */
    JiaoPai(71),
    /**
     * 投降
     */
    TouXiang(72),
    /**
     * 不投降
     */
    BuTouXiang(73),
    /**
     * 报春
     */
    BaoChun(74),
    /**
     * 抄底
     */
    ChaoDi(75),
    /**
     * 不抄底
     */
    BuChaoDi(76),
    /**
     * 天外天
     */
    TianWaiTian(77),
    /**
     * 天独
     */
    TianDu(78),
    /**
     * 独牌
     */
    DuPai(79),
    /**
     * 不独
     */
    BuDu(80),
    /**
     * 要不起
     */
    YaoBuQi(81),
    /**
     * 等待操作
     */
    Wait(82),
    /**
     * 罚牌
     */
    Punish(83),
    /**
     * 素包
     */
    SuBao(84),
    /**
     * 硬包
     */
    YingBao(85),
    /**
     * 反包
     */
    FanBao(86),
    /**
     * 包庄
     */
    BaoZhuang(87),
    /**
     * 抢独
     */
    QiangDu(88),
    /**
     * 不抢独
     */
    BuQiangDu(89),
    /**
     * 登基
     */
    DengJi(90),
    /**
     * 让位
     */
    RangWei(91),
    /**
     * 选侍卫
     */
    XuanShiWei(92),
    /**
     * 明独
     */
    MingDu(93),
    /**
     * 暗独
     */
    AnDu(94),
    /**
     * 明保
     */
    MingBao(95),
    /**
     * 暗保
     */
    AnBao(96),
    /**
     * 亮红十
     */
    LiangRedTen(97),
    /**
     * 不亮
     */
    BuLiang(98),
    /**
     * 扎股
     */
    ZhaGu(99),
    /**
     * 亮三
     */
    LiangSan(100),
    /**
     * 胡
     */
    Hu(101),
    /**
     * 踢
     */
    Ti(102),
    /**
     * 不踢
     */
    BuTi(103),
    /**
     * 造反
     */
    ZaoFan(104),
    /**
     * 不造反
     */
    BuZaoFan(105),
    /**
     * //加注
     */
    ADD_BET_ONE(106),
    ADD_BET_TWO(107),
    ADD_BET_FOUR(108),
    ADD_BET_WHAT(109),
    LiPai(110),
    Buy(111),
    /**
     * 开始
     */
    KaiShi(112),
    /**
     * 叫主
     */
    JiaoZhu(113),
    /**
     * 不叫主
     */
    BuJiaoZhu(114),
    /**
     * 埋底
     */
    MaiDi(115),
    /**
     * 反主
     */
    FanZhu(116),
    /**
     * 不反主
     */
    BuFanZhu(117),
    /**
     * 搓牌
     */
    CuoPai(118),
    /**
     * 看牌
     */
    KanPai(119),
    /**
     * 开牌
     */
    KaiPai(120),
    /**
     * 加注
     */
    JiaZhu(121),
    ZhaoPai(123),
    BuZhao(124),
    TingPai(122),
    FeiPai(126),//飞牌
    FeiPaiTwo(127),//飞牌
    TongYi(128),//同意
    JuJue(129),//拒绝
    FaQi(130),//发起
    FaQiPass(131),//发起
    ShaoPai(132),//烧牌
    BuShaoPai(133),//不烧牌
    JiaShao(134),//假烧
    /**
     * 不硬包
     */
    BuYingBao(135),
    Cha(136),//叉
    ZhaoPy(137),//找朋友

    /**
     * 换日
     */
    HuanRi(138),
    /**
     * 心生
     */
    XinSheng(139),
    /**
     * 让牌
     */
    RangPai(140),
    /**
     * 报数
     */
    BaoShu(141),
    /**
     * 不报
     */
    BuBao(142),

    BaoXi(143);

    private int value;

    private PKOpType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static PKOpType valueOf(int value) {
        for (PKOpType flow : PKOpType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return PKOpType.Not;
    }

}

