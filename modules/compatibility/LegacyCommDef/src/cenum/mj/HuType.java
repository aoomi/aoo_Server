package cenum.mj;

public enum HuType {
    NotHu(0), // 没胡
    ZiMo(1), // 自摸
    QGH(2), // 抢杠胡
    FHZ(3), // 4红中
    SanJinDao(4), // 三金倒
    DanYou(5), // 单游
    ShuangYou(6), // 双游
    SanYou(7), // 三游
    QiangJin(8), // 抢金
    SiJinDao(9), // 四金
    WuJinDao(10), // 五金
    LiuJinDao(11), // 六金
    ShiSanYao(12), // 十三幺
    DDHu(13), // 对对胡
    TianHu(14), // 天胡
    PingHu(15), // 平胡
    JinQue(16), // 金雀
    JinLong(17), // 金龙
    YiZhangHua(18), // 一张花
    WHuaWGang(19), // 无花无杠
    HunYiSe(20), // 混一色
    QingYiSe(21), // 清一色
    JinGang(25), // 金杠
    DiHu(26), // 地胡
    XDDHu(27), // 小对对胡
    DaDuiPeng(28), // 大对碰
    DaSanYuan(29), // 大三元
    SanJinYou(30), // 三金游
    DianPao(31), // 点炮
    DanDiao(32), // 单吊
    LuanFeng(33), // 乱风
    CS_Tou(34), // 财神头
    SC_Ke(35), // 三财一刻
    SSBK(36), // 十三不靠
    SSBK_Qing(37), // 十三不靠清
    PiHu(38), // 屁胡
    MenZi(39), // 门子
    JieDao(40), // 接刀
    GSKH(41), // 杠上开花
    BuQiuR(42), // 不求人 胡牌
    KouTing(43), // 扣听
    HaiDiLao(44), // 海底捞
    QingLong(45), // 青龙
    SanAnKe(46), // 三暗刻
    ZhuoPao(47), // 捉炮
    ShouBaYi(48), // 手把一
    JiangYiSe(49), // 将一色
    QYS_PPHu(50),
    JiePao(52),//接炮
    QuanFeng(53),//全风
    BaHua(54),//八花
    MenQing(55),//门清
    GangChong(56),//杠冲
    YiPaoDuoXiang(57),//杠冲
    //血战类游戏使用
    HuOne(58),//一胡牌
    HuTwo(59),//二胡牌
    HuThree(60),//三胡牌
    HuFour(69),//四接炮
    HuFive(70),//五接炮
    ZiMoOne(61),//一自摸
    ZiMoTwo(62),//二自摸
    ZiMoThree(63),//三自摸
    ZiMoFour(71),//四自摸
    ZiMoFive(72),//五自摸
    ChaHuaZhu(73),//查花猪
    ChaJiao(64),//查叫
    //宁波麻将
    GuanZhan(65),//观战 不参与战斗
    //贵阳捉鸡
    GSP(66),//杠上炮
    HuangZhuang(67),//荒庄流局
    JieSan(68),//解散
    SiYou(74),//四游
    BaYou(75),//八游
    ShiLiuYou(76),//十六游
    SanShiErYou(77),//三十二游
    QiangJinNew(78), // 抢金
    GangShangHua(79), // 杠上花
    ChuChong(80), // 出冲
    ChaGuanSi(81), // 查关死
    QiangTiHu(82),//抢提胡
    ;
    private int value;

    private HuType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static HuType valueOf(int value) {
        for (HuType flow : HuType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return HuType.NotHu;
    }
}
