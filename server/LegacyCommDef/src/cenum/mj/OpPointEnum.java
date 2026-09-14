package cenum.mj;

/**
 * 以 ‘_’开头的表示财归（无金）。
 *
 * @author Administrator
 */
public enum OpPointEnum {
    Not, // 没有胡
    Hu, // 胡
    JieGang, // 明杠
    JieGangNum, // 明杠数量
    Gang, // 补杠
    GangNum,//补杠数量
    AnGang, // 暗杠
    AnGangNum,//暗杠数量
    QYS, // 清一色
    TianHu, // 天胡
    ZiMo, // 自摸
    QGH, // 抢杠胡
    JinGang, // 金杠
    JinLong, // 金龙
    JinQue, // 金雀
    SanJinDao, // 三金倒
    WuJinDao, // 五金倒
    LiuJinDao, // 六金倒
    Jin, // 金番
    DiHu, // 地胡
    DiFen, // 底分
    LianZhuang, // 连庄
    SiJinDao, // 四金倒
    DDHu, // 对对胡
    HDDHu,        //豪华七小对
    CHDDHu,        //超级豪华对对胡
    CCHDDHu,    //超超级豪华对对胡
    _DDHu, // 对对胡
    PaoHu, // 炮胡
    PingHu, // 平胡
    Point, // 分数
    HYS, // 混一色
    WHuaWGang, // 无花无杠
    YiZhangHua, // 一张花
    QiangJin, // 枪金
    Hua, // 花番
    Zhuang, // 庄家
    Xian, // 闲家
    ZhaMa, // 扎码
    PPHu,    //碰碰胡
    QDHu,    //七对胡
    SSL,    //十三烂
    QSSL,    //清十三烂
    ZYS,    //字一色
    QQR,    //全球人
    ShangHuo,//上火
    PiaoFen,//漂分
    FHZ,        // 四红中
    BQGH,        // 被抢杠胡扣分
    Tong,        // 打同牌分：立即支付给每家2点；
    Gen,        // 跟牌分：普通牌：立即支付给每家1点；
    GenJin,        // 跟牌分：百搭牌-百搭牌：立即支付给每家2点；
    TouDa,        // 头搭
    ErDa,        // 二搭
    PengDa,        // 碰后搭
    TuoDa,        // 脱搭
    PaoDa,        // 跑搭
    TDPD,        // 脱搭跑搭
    PDTD,        // 跑搭脱搭
    WeiPai,        // 喂牌
    ZGKG,        // 直杠杠开
    WGKG,        // 弯杠杠开
    AGKG,        // 暗杠杠开
    TD,            // 脱搭
    PD,            // 跑搭
    TDP,        // 脱搭跑搭
    PDT,        // 跑搭脱搭
    GK,            // 杠开
    ShuangShai, // 双

    DanYou, // 单游
    ShuangYou, // 双游
    SanYou, // 三游
    SanJinYou, // 三金游
    HDLY,        // 海底捞月
    GSKH,        // 杠上开花
    GSP,        // 杠上炮
    ZhongNiao,    // 中鸟
    NiaoPai,    // 鸟牌
    FangGang,    // 放杠
    JiePao,        // 接炮

    DianPeng,    // 点碰
    DianGang,    // 点杠
    LuoHan18,    // 18罗汉
    LanHu,        // 烂胡
    LuanJiang,    // 烂将
    Long,    // 一条龙
    DaTou,    // 打骰
    MaiZi,    // 买子
    DianPao,    //点炮
    DD,            //大吊车（独钓）
    SiBao,        //四宝
    MenQing,    //门清
    LuanFeng, //乱风
    PPHuQuanFeng,//碰碰胡全风
    QDHuQuanFeng,//七对胡全风
    DuDiao,    //独钓
    BaHua,        //八花
    PiaoHua, //飘花
    HuPoint,    //胡分
    FaBao,//罚宝
    FeiLong,//四宝飞龙
    FeiBao,//飞宝
    DouBao,//兜宝
    HunYou,//混悠
    SPHZ,//四牌黄庄，不算牌型分，但是庄家需要给闲家每人2分
    HZ,// 摸完所有牌黄庄时，不算牌型分，但是闲家每人需要给庄家2分
    SSY,// 	十三幺：东、南、西、北、中、发、白板、一九万、一九条、一九筒  这十三张牌里任意一张牌组成对，而另十二张各一张，共计十四张，即构成十三幺，不需要组成胡牌牌型；
    QDD,                //清对对

    LangPai,// 浪牌
    QuanYao,// 全幺
    QiZiQuan,//七字全
    QuanZi,//全字
    WuJingHu,//无精胡
    JingHuanYuan,//精还原
    WuDangDiHu,//无当地胡
    WuDangTianHu,//无当天胡
    PiaoJinFen,//飘精分
    PiaoJinSize,//飘精数
    WDGSKH,// 无当杠上开花
    MenQingPPHu,//门清碰碰胡
    Jian4cheng2,//见4乘2
    TianTing,    // 天听
    DaSanYuan,        //大三元
    XiaoSanYuan,        //小三元
    DaSiXi,       //大四喜
    XiaoSiXi,            //小四喜
    SanAnKe,       //三暗刻
    SiAnKe,       //四暗刻
    WuAnKe,       //五暗刻
    HuaYS,       //花一色
    MenQianQing,            //门前清
    PPH,        //碰碰胡
    WuHuaWuZi,            //无花无字
    BaiLiu,       //百六
    DanTing,       //单听
    BHZM,       //补花自摸
    HDL,        //海底捞月
    QuanQiuR,            //全求人
    AnKe,       //暗刻
    MingKe,       //明刻
    ZiPai,                   //字牌
    TotalTai,                //总台数
    QLHu,//清一色一条龙

    HeiYiSe,//黑一色
    LvYiSe,//绿一色
    XiaoYao,//小幺

    HongHuaSize,// 红
    HeiHuaSize,// 黑
    YaDang,//压挡
    SYZhi,//十一支
    SiHuo,//四活
    DuanDui,//断对
    BQRen,//不求人
    QingDi,//清底
    KZYa,//枯枝压
    PiaoZi,//漂子
    Fa4,//4张发财
    DPYD,//点炮平胡
    DPPH,//点炮压档
    ZMPH,//自摸平胡
    ZMYD,//自摸压挡
    QYSZJ, // 清一色真将


    JYSPPH, // 将一色碰碰胡
    QYSYTL, // 清一色一条龙
    LuanJiangQD,//乱将胡七对
    QYSZJYTL, // 清一色真将一条龙
    ZYSPPH,//子一色碰碰胡
    ZYSHDDHu,        //子一色豪华七对
    ZYSCHDDHu,        //子一色双豪华七对
    LuanJiangCCHDDHu,        //乱将胡三豪华七对
    QYSCCHDDHu,        //清一色三豪华七对
    ZYSCCHDDHu,        //字一色三豪华七对
    QYSPPH,        //清一色碰碰胡
    QYSZJPPH,        //清一色真将碰碰胡
    QYSQD,        //清一色七对
    QYSZJQD, // 清一色真将七对
    QYSHDDHu, // 清一色豪华七对
    QYSCHDDHu, // 清一色双豪华七对
    QYSZJHDDHu, // 清一色豪华七对带真将
    QYSZJCHDDHu,// 清一色双豪华七对带真将
    QYSZJCCHDDHu,// 清一色三豪华七对带真将
    LuanJiangCHDDHu,        //乱将胡双豪华七对
    LuanJiangHDDHu,        //乱将胡豪华七对

    DaFengHu,//胡大风
    XiaoFengHu,//胡小风
    DaFeng,// 大风
    XiaoFeng,//小风

    ShouZhuaYi,// 手抓一
    SiMingGuiYi,//四明归一（全频道）
    AnSiGuiYi,//暗四归一（全频道）

    SiMingGuiYiB,//四明归一（半频道）
    AnSiGuiYiB,//暗四归一（半频道）

    KaWuXing,//卡五星
    LiangDao,//亮倒
    MaiMa,// 买马

    BaoHu,//包胡

    //翻金部分
    DuanDuan,//断断
    YBGao,//一板高,
    EBGao,//二板高,
    JJYao,//就就幺
    WuZiJJYao,//无字就就幺
    RPPeng,//软碰碰
    SGYi,//四归一
    SGEr,//四归二
    LiangGeSGEr,//两个四归二
    SGSan,//四归三
    SGSi,//四归四
    SBTong,//三版同（顺子三版同）
    KSBTong,//三碰碰（刻子三版同）
    GLang,//国浪
    SSLang,//十三浪
    XBB,//小板板
    DBB,//大板板
    DXBB,//大小板板
    LunHu,//轮胡
    SBTongPPHu,//三板同碰碰胡
    QDLunHuEBGao,//七对轮胡两板高
    MGSKH,    // 杠上开花（明杠）
    AGSKH,//杠上开花（暗杠）
    MenQingSSY,//门清十三幺
    JieJing,//接精分
    WuDangDiDuan,//无当地断
    WuDangTianDuan,//无当天断
    DiDuan,//地断
    TianDuan,//天断
    SSSY,//下水十三幺
    HUHUANG_ZHUANG,//荒庄
    TIAN_GANG,//天杠
    QYSQG,//清一色抢杠
    QYSDDP,//清一色大对碰
    QYSDSY,//清一色大三元
    QYSDD,//清一色单吊
    FB,//分饼
    QXJY,//七星九幺
    HHJY,//豪华九幺
    BD,//宝吊
    BHY,//宝还原
    GB,//杠宝
    BDBHY,//宝吊宝还原
    ChaHu,//插胡
    HuXiNum,//胡息
    ChaKan,//插坎
    BanZi,// 搬子
    PeiZi,//配子
    PengBanZi,//碰搬子
    AnGangPeiZi,// 暗杠配子
    FengQing,//风清
    SanZha,// 三炸
    ShuangZha,//双炸
    ZhaQDHu,//炸七对
    CSB,//吃三比
    KanFen,//坎分
    OtherPoint,//其他分
    DianHu,//点胡
    HongHu,//红胡
    HeiHu,//黑胡
    HongZhuanDian,//红转点
    HongZhuanHei,//红转黑
    WangDiao,//王钓
    WangDiaoWang,//王钓王
    WangChuang,//王闯
    WangBa,//王霸
    WuHu,//乌胡
    SBD,//十八大
    SLX,//十六小
    TunNum,//囤
    AN_GANG_ZHUANG_MING_GANG,//暗杠转明杠

    QueYiSe,//缺一色
    BanBanHu,//板板胡
    JiangJiangHu,//将将胡

    JingFen,// 精分
    JZ,//绝张
    AJ,//暗绝
    JiaPai,//架牌
    DaHe,//打和

    PaPoBeiShu,//爬坡倍数
    ZiMoJiaOne,//自摸加1
    DaHongHu,//大红胡
    XiaoHongHu,//小红胡
    SBX,//十八小


    //区分操作者  以便查找
    //-----------------------------==leo_wi==>  begin-------------------
    RenHu,//人胡
    WuHua,//无花
    GangPao,//杠跑
    QiDuiJiaBei,//七对加倍
    GSHJiaBei,//杠上花加倍
    ZiMoJiaBei,//自摸加倍
    PaoFen,//跑分
    HunXing,//混星
    QingXing,//清星
    ZiXing,//字星
    HunTaiKong,//混太空
    QingTaiKong,//清太空
    ZiTaiKong,//字太空
    LuanTaiKong,//乱太空
    HunLong,//混龙
    QingLong,//清龙
    HunXingQuanQiuRen,//混星全求人
    QingXingQuanQiuRen,//清星全求人
    ZiXingQuanQiuRen,//字星全求人
    LuanQuanQiuRen,//乱星全求人
    DeGuo,//德国
    ShuangGu,//双股
    BaoSanJia,//包三家
    FengXiang,//风向
    BianJiaDiao,//边夹吊
    HongQiPP,//红旗飘飘
    XiaoSa,//潇洒
    JiaGun,//加滚
    Zhong,//中
    BaiBan,//白板
    GenZhuangShengJi,//跟庄升级
    HuangZhuangShengJi,//黄庄升级
    ChiFaCai,//吃发财
    ShengJi,//升级
    FaFen,//罚分
    HuaGang,//花杠
    YingHua,//硬花
    RuanHua,//软花
    QYSKuaiZHao,//清一色快照
    KuaiZHao,//普通快照
    HYSDDP,//混一色对对碰
    HYSHDDHu, // 混一色豪华七对
    HYSDDHu, // 混一色七对
    ZuoYa,//做鸭
    YingBaDui,//硬八对
    RuanBaDui,//软八对
    CaiShenNiu,//财神牛
    MaiDi,// 买底
    DingDi,// 顶底
    QuanFeng,// 圈风
    MenFeng,// 门风
    Huang,// 晃
    ShuHuZi,// 数胡子
    ZYSJiaHu,//字一色假胡
    QXSSL,//七星十三烂
    QYSJiaHu,//清一色假胡
    CardTypePoint,//牌型分
    GuJiang,//孤将
    QueLiangMen,//缺两门
    YaoJi,//幺鸡
    BenJi,//本鸡
    FanJi,//翻鸡
    FanJi1,//翻鸡1
    WuGuJi,//乌骨鸡
    ChongFengJi21,//冲
    ChongFengJi38,//冲8
    JinJi21,//金鸡21
    JinJi38,//金鸡38
    YinJi31,//银鸡31
    YinJi11,//银鸡11
    ShaBao,//杀报
    Ze21,//责
    Ze38,//责8
    JiuPengBaoDeng,//九蓬宝灯
    LianQiDui,//连七对
    SiGang,//四杠
    QingYaoJiu,//清幺九
    YiSeShuangLong,//一色双龙
    YiSeSiJieGao,//一色四节高
    YiSeSiTongShun,//一色四同顺
    YiSeSiBuGao,//一色四步高
    SanGang,//三杠
    HunYaoJiu,//混幺九
    QiXingBuKao,//七星不靠
    QuanShuangKe,//全双刻
    YiSeSanTongShun,//一色三同顺
    YiSeSanJieGao,//一色三节高
    QuanDa,//全大
    QuanZhong,//全中
    QuanXiao,//全小
    SanSeShuangLongHui,//三色双龙会
    YiSeSanBuGao,//一色三步高
    QuanDaiWu,//全带五
    QuanDaiYao,//全带幺
    SanTongKe,//三同刻
    QuanBuKao,//全部靠
    ZuHeLong,//组合龙
    DaYuWu,//大于五
    XiaoYuWu,//小于五
    SanFengKe,//三风刻
    HuaLong,//花龙
    TuiBuDao,//推不倒
    SanSeSanTongShun,//三色三同顺
    SanSeSanJieGao,//三色三节高
    MiaoShouHuiChun,//妙手回春
    SanSeSanBuGao,//三色三步高
    WuMenQi,//五门齐
    ShuangAnGang,//双暗杠
    ShuangMingGang,//双明杠
    ShuangJianKe,//双箭刻
    MingAnGang,//明暗杠
    HeJueZhang,//和绝张
    JianKe,//箭刻
    PingHe,//平和
    WuFanHe,//无翻和
    SiGuiYi,//四归一
    ShuangTongKe,//双同刻
    ShuangAnKe,//双暗刻
    DuanYao,//断幺
    YiBanGao,//一般高
    XiXiangFeng,//喜相逢
    LianLiu,//连六
    LaoShaoFu,//老少副
    WuZi,//无字
    BianZhang,//边张
    KanZhang,//坎张
    YaoJiuKe19,//幺九刻
    YaoJiuKeZi,//幺九刻
    DDGK,//单钓杠开
    PPHuGK,//碰碰胡杠开
    PingHuTianHu,//平胡天胡
    PPHuTianHu,//碰碰胡天胡
    QDHuTianHu,//七对天胡

    //-----------------------------==leo_wi==>  end-------------------
    ZhangZhang8,//8张涨：胡牌时手里万筒条中有一门的张数大于等于8张；
    DuYing,//独赢：只听一张牌的情况下胡牌（单吊、边张、夹子）；
    YaoSanJiu,// 幺三九
    WuQiJiu,// 五七九
    ZhongFaBai,// 中发白
    LuanSanFeng,// 乱三风
    FengKan,// 风坎
    BanDao,// 扳倒：只要有胡牌就有此嘴，1嘴；
    ShuaiZhang,// 甩张
    YiZhiHua,// 一枝花
    QDJiaBei,// 七对加倍
    WuGuiBei,// 无鬼加倍
    XingFen,// 醒分


    YingHu, // 硬胡
    DuiKaiKou,// 对开口
    KaiKou,//开口
    HongZhongGang, // 红中杠
    FaCaiGang,// 发财杠
    LaiZiGang, // 赖子杠
    ZYSQD,//字一色七对
    ZhuoWuKui,//捉五魁
    XSQD,//潇洒七对
    XSQDDYG,//潇洒七对带一滚
    XSQDDEG,//潇洒七对带二滚
    XSQDDSG,//潇洒七对带三滚
    KaErTiao,//卡二条
    HaiDiPao,//海底炮
    BaoJiao,//报叫
    ChaGuanSi,//查关死
    ChaDaJiao,//查大叫
    BaoTou,//暴头
    GangKai,//杠开
    DaDiao,//大吊
    GangBao,//杠爆
    CaiPiao,//财飘
    DaDiaoBaoTou,//大吊爆头
    DaDiaoGangKai,//大吊杠开
    DaDiaoGangBao,//大吊杠爆
    DaDiaoCaiPiao,//大吊财飘
    PiaoGangBao,//飘杠爆
    GangCaiPiao,//杠财飘
    DiHua,//底花
    HuaShu,//花数
    WuHuaWuCai,//无花无财
    ShunHu, // 顺胡
    ZhongZhang,//中张
    WuJi,//无鸡
    JinGouDiao,//金钩钓
    DeZhongDe,//德中德
    ChaoZhuang,//抄庄
    QingDui,//清对
    QingQiDui,//清七对
    QingLongQiDui,//清龙七对
    ChaJiao,//查叫
    ChongGuan,//冲关
    LiuJuFen,//流局分
    SanHong,//三红
    WenQian,//文钱
    WenQian2,//二文钱
    WenQian3,//三文钱
    WenQian4,//四文钱
    QiongHen,//穷狠
    QiongQiongHen,//四文钱
    JiangDui,//将对
    JinGouPao,//金钩炮
    BaiDuZhang,//摆独张
    BaiPai,//摆牌
    KaXinWu,//卡心五
    ;
}