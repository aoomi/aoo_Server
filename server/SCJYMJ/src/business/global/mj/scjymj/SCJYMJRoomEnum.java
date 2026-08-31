package business.global.mj.scjymj;

import java.util.Arrays;
import java.util.List;

/**
 * 安岳麻将
 *
 * @author Administrator
 */
public class SCJYMJRoomEnum {
    public static final List<Integer> WA_WU = Arrays.asList(14, 15, 16);
    public static final List<Integer> TI_WU = Arrays.asList(24, 25, 26);
    public static final List<Integer> TO_WU = Arrays.asList(34, 35, 36);

    /**
     *  可选玩法：点炮可平胡、板板高、夹心五、金钩钓、海底捞、一条龙、独张、天地胡、杠上跑转雨；  复选项，可多选，默认全部勾选； 
     * 每种玩法胡牌都是加1番，可叠加；  点炮可平胡：平胡也可点炮；  板板高：比如：同一门花色的778899 等连续3对牌，共计6张牌叫做板板高；
     *  夹心五：同一门花色4和6，下叫，胡牌只有夹心5。只能胡5的夹心牌就叫夹心五（小七对和龙七对除外）；  金钩钓：碰牌，杠牌后，手上只剩一张牌；
     *  海底捞：最后一张牌自摸；  一条龙：同一门花色的123456789等连续牌叫一条龙 
     * 独张：牌桌上已经出现了同一门花色的3张一样的牌（手上的牌不算），最后剩下的第四张牌就是独张；  *天地胡： 
     * 杠上跑转雨：被抢杠的这个杠的分也转给抢杠者；
     *
     * @author Administrator
     */
    public enum SCJYMJCfg {
        // 点炮可平胡
        DianPaoPingHu,
        // 板板高
        BanBanGao,
        // 夹心五
        JiaXinWu,
        // 金钩钓
        JinGouDiao,
        // 海底捞
        HaiDiLao,
        // 一条龙
        Long,
        // 独张
        DuZhang,
        // 天地胡
        TianDiHu,
        // 杠上跑转雨
        GangShangPaoZhuanYu,
        // 碰杠
        PengGang,
        // 海底杠
        HaiDiGang,
        Cagua,
        YaoJiu;
    }

    /**
     *  其他：自动准备，庄闲玩法;  复选项，单选，默认勾选；  自动准备：勾选后玩家加入房间时，自动进行准备，未勾选则需要手动准备；默认不勾选
     *  庄闲玩法：庄家胡牌，闲家需多给1分，闲家胡牌，庄家需多出1分； *
     *
     * @author Administrator
     */
    public enum SCJYMJQiTa {
        ZiDong, ZhuangXian,
        ;
    }

    /**
     *  牌张：2房牌、3房牌；  默认2房牌；
     *
     * @author Administrator
     */
    public enum SCJYMJFangZhang {
        PAI_2, PAI_3,
        ;

        private SCJYMJFangZhang() {
        }

        public static SCJYMJFangZhang valueOf(int value) {
            for (SCJYMJFangZhang huPai : SCJYMJFangZhang.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJFangZhang.PAI_2;
        }
    }

    /**
     * 胡牌：自摸加底、自摸加番；  单选，默认自摸加番；
     *
     * @author Administrator
     */
    public enum SCJYMJHuPai {
        ZiMoFan, ZiMoDi,
        ;

        private SCJYMJHuPai() {
        }

        public static SCJYMJHuPai valueOf(int value) {
            for (SCJYMJHuPai huPai : SCJYMJHuPai.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJHuPai.ZiMoFan;
        }
    }

    /**
     *  封顶：2番，3番，4番  默认4番；  结合飘在内或飘在外结算番数； *
     *
     * @author Administrator
     */
    public enum SCJYMJFengDing {
        Fan2(4), Fan3(5), Fan4(20);
        int value;

        private SCJYMJFengDing(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SCJYMJFengDing valueOf(int value) {
            for (SCJYMJFengDing huPai : SCJYMJFengDing.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJFengDing.Fan4;
        }
    }

    /**
     *  查叫：差大，查小 *
     *
     * @author Administrator
     */
    public enum SCJYMJDianGangHua {
        Zimo(), DianPao();
        int value;

        private SCJYMJDianGangHua() {

        }

        public int value() {
            return this.value;
        }

        public static SCJYMJDianGangHua valueOf(int value) {
            for (SCJYMJDianGangHua huPai : SCJYMJDianGangHua.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJDianGangHua.Zimo;
        }
    }

    /**
     *  查叫：差大，查小 *
     *
     * @author Administrator
     */
    public enum SCJYMJChajiao {
        Chada(), Chaxiao();
        int value;

        private SCJYMJChajiao() {

        }

        public int value() {
            return this.value;
        }

        public static SCJYMJChajiao valueOf(int value) {
            for (SCJYMJChajiao huPai : SCJYMJChajiao.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJChajiao.Chada;
        }
    }


    public enum SCJYMJJieSanShu {
        Jie2(2), Jie3(3), Jie5(4);
        int value;

        private SCJYMJJieSanShu(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SCJYMJJieSanShu valueOf(int value) {
            for (SCJYMJJieSanShu huPai : SCJYMJJieSanShu.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJJieSanShu.Jie2;
        }
    }

    /**
     *  飘：飘在内（选飘）、飘在外（选飘）飘在内（座飘）、飘在外（座飘）、不飘  *默认飘在内（座飘）；  *选飘：第一局开始选择飘还是不飘； 
     * *座飘：默认飘，不能选择；  *飘在内：封顶包含飘（杠分，庄家分另外算）；  *飘在外：封顶不含飘分，另外加算飘分（杠分，庄家分另外算）；
     *
     * @author Administrator
     */
    public enum SCJYMJPiaoWanFa {
        Piao_Net_Xuan, Piao_Wai_Xuan, Piao_Net_Zuo, Piao_Wai_Zuo, Bu_Piao,
        ;

        private SCJYMJPiaoWanFa() {
        }

        public static SCJYMJPiaoWanFa valueOf(int value) {
            for (SCJYMJPiaoWanFa flow : SCJYMJPiaoWanFa.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return SCJYMJPiaoWanFa.Piao_Net_Zuo;
        }
    }

    /**
     * 飘，过
     *
     * @author Administrator
     */
    public enum SCJYMJPiao {
        // 错误
        Error(-1),
        // 过
        Pass(0),
        // 飘
        Piao(1),
        ;
        private int value;

        private SCJYMJPiao(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static SCJYMJPiao valueOf(int value) {
            for (SCJYMJPiao flow : SCJYMJPiao.values()) {
                if (flow.value() == value) {
                    return flow;
                }
            }
            return SCJYMJPiao.Error;
        }
    }

    /**
     * 不限制,30秒出牌,60秒出牌
     *ti
     * @author Administrator
     */
    public enum SCJYMJXianShi {
        NOT(0), // 不限制
        SHi_30(15100), // 15秒出牌
        SHI_60(60100),// 60秒出牌
        SHI_120(120100),// 120秒出牌
        SHI_240(240100),// 240秒出牌
        SHI_360(360100),// 360秒出牌
        ;
        private int value;

        private SCJYMJXianShi(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SCJYMJXianShi valueOf(int value) {
            for (SCJYMJXianShi huPai : SCJYMJXianShi.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return SCJYMJXianShi.NOT;
        }
    }

    /**
     * 动作分数
     *
     * @author Administrator
     */
    public enum SCJYMJOpPoint {
        Not(0),
        //	平胡0番（x1）；
        PingHu(0),
        //	天胡、地胡；
        TianHu(4),
        DiHu(4),
        // 	对对胡1番（x2）；
        DDHu(1),
        // 	清一色、七对3番（x8）；
        QYS(2),
        QiDuiHu(2),
        // 	清一色对对胡、龙七对 3番（x8）；
        QYSDDHu(3),
        LongQiDuiHu(2),
        //     清一色七对 4番（x16）；
        QYSQiDuiHu(4),
        //	    清一色龙七对5番（x32）；
        QYSLongQiDuiHu(4),
        YaoJiu(4),
        QYaoJiu(6),
        // 杠上开花
        GSKH(1),
        // 杠上炮
        GSP(1),
        // 抢杠胡
        QGHu(1),
        // 自摸自抢杠
        ZiMoZiQG(1),
        // 海底捞月
        HDLY(1),
        // 查花猪
        ChaHuaZhu(0),
        // 查大叫,
        ChaDaJiao(0),
        // 反查
        FanCha(0),
        // 暗杠
        AnGang(0),

        //杠
        Gang(0),

        //接杠
        JieGang(0),

        // 点杠
        DianGang(0),
        // 接炮
        JiePao(0),
        // 点炮
        DianPao(0),
        // 自摸
        ZiMo(0),
        // 金钩钓
        JinGouDiao(0),
        // 独张
        DuZhang(0),
        // 一条龙
        Long(0),
        // 板板高
        BanBanGao(0),
        // 夹心五
        JiaXinWu(0),
        // 杠上炮转雨
        GangShangPaoZhuanYu(0),
        // 根
        Gen(0),
        // 爆听
        BaoTing(0),
        // 擦刮
        CaGua(0);

        private int value;

        private SCJYMJOpPoint(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

    }

    ;


    /**
     * 动作类型值
     *
     * @return
     */
    public static int OpValue(SCJYMJOpPoint type) {
        switch (type) {
            case ChaHuaZhu:
                return 16;
            case AnGang:
                return 2;
            case Gang:
            case CaGua:
                return 1;
            case JieGang:
                return 2;
            case JinGouDiao:
            case DuZhang:
            case Long:
            case BanBanGao:
            case GSKH:
            case HDLY:
            case QGHu:
            case GSP:
            case JiaXinWu:
                return 1;


            default:
                break;
        }
        return 0;

    }

    /**
     * 保定麻将
     *
     * @author Administrator
     */
    public enum SCJYMJEndType {
        /**
         * 不特殊操作
         */
        NOT,
        /**
         * 加
         */
        PLUS,
        /**
         * 乘
         */
        MULTIPLY,
        ;
    }
}
