package business.global.mj.template;


/**
 * 保定易县麻将配置
 *
 * @author Administrator
 */
public class MJTemplateRoomEnum {
    /**
     * 无,本金是金3张，本金是金4张，退金4张，进金4张，本金+进金7张，本金+进金8张，
     * 本金+退金7张，本金+退金8张，进金+退金8张，进金+退金+本金11张，进金+退金+本金12张，
     */
    public enum KaiJinWanFa {
        NOT(0),
        BENJIN_3ZHANG(1),
        BENJIN_4ZHANG(1),
        BACKJIN_4ZHANG(1),
        JINJIN_4ZHANG(1),
        BENJIN_ADD_JINJIN_7ZHANG(2),
        BENJIN_ADD_JINJIN_8ZHANG(2),
        BENJIN_ADD_BACKJIN_7ZHANG(2),
        BENJIN_ADD_BACKJIN_8ZHANG(2),
        BACK_JIN_ADD_JIN_JIN_8ZHANG(2),
        JIN1_ADD_JIN2_8ZHANG(2),//翻开一张牌，该牌的顺一位和二位做为混牌；
        BACK_JIN_ADD_BENJIN_ADD_JIN_JIN_11ZHANG(3),
        BACK_JIN_ADD_BENJIN_ADD_JIN_JIN_12ZHANG(3),
        BENSHEN_JIN1_ADD_JIN2_11ZHANG(3),//翻开一张牌，该牌及其顺一位和二位做为混牌；

        ;
        public int kaiJinNum;

        KaiJinWanFa(int kaiJinNum) {
            this.kaiJinNum = kaiJinNum;
        }

        public static KaiJinWanFa valueOf(int odinal) {
            for (KaiJinWanFa flow : KaiJinWanFa.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return KaiJinWanFa.NOT;
        }
    }

    /**
     * 普通模式,血流模式,血战模式
     */
    public enum MoShi {
        NORMAL,
        XUE_LIU,
        XUE_ZHAN,
        ;

        public static MoShi valueOf(int odinal) {
            for (MoShi flow : MoShi.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return MoShi.NORMAL;
        }
    }

    /**
     * 飘分
     */
    public enum PiaoFen {
        NOT,
        PIAO_FEN_FIRSER_SET,//首轮票分
        PIAO_FEN_EACH_SET,//每没轮票分
        ;

        public static PiaoFen valueOf(int odinal) {
            for (PiaoFen flow : PiaoFen.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return PiaoFen.NOT;
        }
    }

    /**
     * 飘分个数 无，自由选，固定1 固定2 固定3
     */
    public enum PiaoFenNum {
        NOT(-1),
        ZI_YOU_XAUN(-1),//
        PIAO1(1),
        PIAO2(2),
        PIAO3(3),
        PIAO4(4),
        PIAO5(5),
        ;
        public int value;

        PiaoFenNum(int value) {
            this.value = value;
        }

        public static PiaoFenNum valueOf(int odinal) {
            for (PiaoFenNum flow : PiaoFenNum.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return PiaoFenNum.NOT;
        }
    }

    /**
     * 买码数可选
     */
    public enum MaiMa {
        NOT(0),
        MAI_MA2(2),
        MAI_MA4(4),
        MAI_MA6(6),
        MAI_MA8(8),
        MAI_MA10(10),
        MAI_MA12(12),
        MAI_MA14(14),
        MAI_MA16(16),
        MAI_MA18(18),
        MAI_MA20(20),

        ;
        public int value;

        MaiMa(int value) {
            this.value = value;
        }

        public static MaiMa valueOf(int odinal) {
            for (MaiMa flow : MaiMa.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return MaiMa.NOT;
        }
    }

    /**
     * 玩家互换牌的牌型类型
     * 不换张,换张（同色）,换张（不同色）
     */
    public enum ChangeCardType {
        NOT,
        SAME_COLOR,
        DIFFERENT_COLOR,
        ;

        public static ChangeCardType valueOf(int odinal) {
            for (ChangeCardType flow : ChangeCardType.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return ChangeCardType.NOT;
        }
    }

    /**
     * 换张顺序 顺时针 逆时针 对家（4人场才有）,随机1对1
     */
    public enum ChangeCardOderBy {
        SHUN_SHI_ZHEN,
        NI_SHI_ZHEN,
        DUI_JIA,
        RANDOM_1TO1,
        ;

        public static ChangeCardOderBy valueOf(int odinal) {
            for (ChangeCardOderBy flow : ChangeCardOderBy.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return ChangeCardOderBy.SHUN_SHI_ZHEN;
        }
    }

    /**
     * 玩家互换牌的张数
     * 0,三张
     */
    public enum ChangeCardNum {
        NOT(0),
        THree(3),
        ;
        public int value;

        ChangeCardNum(int value) {
            this.value = value;
        }

        public static ChangeCardNum valueOf(int odinal) {
            for (ChangeCardNum flow : ChangeCardNum.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return ChangeCardNum.NOT;
        }
    }

    /**
     * 不定缺，定缺
     */
    public enum DingQue {
        NOT,
        DING_QUE,
        ;

        public static DingQue valueOf(int odinal) {
            for (DingQue flow : DingQue.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return DingQue.NOT;
        }
    }


    public enum HuCardEndType {
        NOT(-1),
        FORTH(4),
        THIRD(3),
        SECOND(2),
        FIRST(1),
        ;
        public int value;

        HuCardEndType(int value) {
            this.value = value;
        }

        public static HuCardEndType valueOf(int odinal) {
            for (HuCardEndType flow : HuCardEndType.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return HuCardEndType.FORTH;
        }

        public static HuCardEndType value2Of(int value) {
            for (HuCardEndType flow : HuCardEndType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return HuCardEndType.FORTH;
        }
    }

    /**
     * 海底捞月
     * 一发一张，然后每人拿这张检测胡
     * 每人发一张，检测胡
     */
    public enum HaiDiLaoYue {
        NOT,
        DEAL_ONE_CARD,
        EACH_DEAL_CARD,
        ;

        public static HaiDiLaoYue valueOf(int odinal) {
            for (HaiDiLaoYue flow : HaiDiLaoYue.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return HaiDiLaoYue.NOT;
        }
    }

    /**
     * 留牌张数额外可选：
     * 方式1：固定可设置留牌张数；
     * 方式2：根据杠牌数量留牌；（参考濮阳麻将）
     * 方式3：根据抓鸟方式留牌；
     */
    public enum LiuPaiExtrasOptions {
        NOT,
        GANG_CARD_LIU_PAI,
        MAI_MA_LIU_PAI,
        ;

        public static LiuPaiExtrasOptions valueOf(int odinal) {
            for (LiuPaiExtrasOptions flow : LiuPaiExtrasOptions.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return LiuPaiExtrasOptions.NOT;
        }

    }


    /**
     * 根据杠牌数量留牌：
     * 每杠一次留几张牌
     */
    public enum GangCardLiuPaiNum {
        NOT(0),
        LIU_1(1),
        LIU_2(2),
        LIU_3(3),
        LIU_4(4),
        ;
        public int value;

        GangCardLiuPaiNum(int value) {
            this.value = value;
        }

        public static GangCardLiuPaiNum valueOf(int odinal) {
            for (GangCardLiuPaiNum flow : GangCardLiuPaiNum.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return GangCardLiuPaiNum.NOT;
        }
    }

    /**
     * 根据杠牌数量留牌：
     * 每杠一次留几张牌
     */
    public enum LiuPai {
        NOT(0),
        LIU_PAI5(5),
        LIU_PAI10(10),
        LIU_PAI15(15),
        LIU_PAI20(20),
        ;
        public int value;

        LiuPai(int value) {
            this.value = value;
        }

        public static LiuPai valueOf(int odinal) {
            for (LiuPai flow : LiuPai.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return LiuPai.NOT;
        }
    }

    /**
     * 选庄家：坐庄家规则；
     * 做庄规则1：不管谁胡牌或是否流局，固定下家做庄，下局庄家为本局庄家的下家；
     * 做庄规则2：有玩家胡牌时，胡牌玩家做庄，即本局谁胡牌则下局庄家为谁，如果庄家胡牌则连庄；
     * 做庄规则3：庄家胡，庄家连庄，其他玩家胡，下家坐庄
     * 注：2与3只能选择一个；
     */
    public enum HuCardLunZhuang {
        XIA_JIA_ZHUANG,
        HU_JIA_ZHUANG,
        ZHUANGHU_LIANZHUANG_ZHUANGBUHU_XIAJIA_ZHUANG,
        ;

    }

    /**
     * 做庄规则4：流局时，庄家连庄；
     * 做庄规则5：流局时，庄家下家做庄；
     * 做庄规则6：流局时，摸最后一张牌的玩家做庄；
     * 注：4、5、6只能选择一个；
     */
    public enum LiuJuLunZhuang {
        ZHUANG_LIAN_ZHUANG,
        XIA_JIA_ZHUANG,
        LAST_POP_CARD_POS_ZHUANG,
        ;
    }

    /**
     * 做庄规则7：一炮多响时，点炮玩家做庄；
     * 做庄规则8：一炮多响时，庄家下家做庄；
     * 做庄规则9：一炮多响时，离点炮玩家近的胡牌玩家做庄；
     * 注：规则789只能选择一个；
     * 做庄规则10：一炮多响时，如果庄家有胡牌，则庄家连庄；
     * 注：如果同时选择了规则10和规则8、9中的一个，则规则10的优先级高；
     * 注：规则10与7不可同时选择；
     */
    public enum YPDXLunZhuang {
        DIAN_PAO_ZHUANG,
        XIA_JIA_ZHUANG,
        NEAREST_DIAN_PAO_ZHUANG,
        ;
    }

    /**
     * 做庄规则10：一炮多响时，如果庄家有胡牌，则庄家连庄；
     */
    public enum YPDXLunZhuang_ZhuangHu {
        NOT,
        YES,
        ;
    }

    /**
     * 摸完最后一张牌后是否打出
     */
    public enum OutLastCard {
        NOT,
        YES,
        ;
    }

    /**
     * 是否又摇杠操作
     */
    public enum YaoGang {
        NOT,
        YAO_GANG,
        ;

    }


    /**
     * 跟庄人数 0为没又跟庄
     */
    public enum GenZhuangPlayerNum {
        NOT(0),
        PLAYER_NUM3(3),
        PLAYER_NUM4(4),
        ;
        public int value;

        GenZhuangPlayerNum(int value) {
            this.value = value;
        }
    }

    /**
     * 实时
     */
    public enum ActualTimeCalcPoint {
        NOT,
        CALC_GANG_POINT,
    }

    /**
     * 漏炮胡枚举
     * 1.无
     * 2.任意玩家打出一张，能炮胡不胡则摸牌（或吃碰杠）前都不能胡
     * 3.任意玩家打出一张，能炮胡不胡，后续玩家都不能胡这张
     * 4.任意玩家打出一张，能炮胡不胡，如果胡牌分比较大可以胡
     * 5.不能胡自己打出的牌
     * 6.能胡不胡 摸牌前都不能胡
     * 7.摸牌（或吃、碰、杠牌）解除,自摸能胡不胡也要算进去
     */
    public enum LouHuEnum {
        NOT,
        DA_PAI_HOU_KE_HU,
        BU_HU_ZHE_ZHANG,
        HU_FEN_DA_KE_HU,
        BU_HU_DA_GUO_DE_PAI,
        NENG_HU_BU_HU_MO_PAI_KE_HU,
        ZiMO_BUHU_SUAN_LOU_HU,
    }

    /**
     * 自动操作类型
     */
    public enum AutoOpType {
        Out,
        Hu,
    }

    /**
     *
     */
    public enum WaitingExType {
        NOT,
        PAO,
        PIAO,
        MAI,
        BAO,

    }

    /**
     * -1 等待操作
     * -2 不用操作
     */
    public enum WaitingExOpType {
        WAITING_OP(-1),
        NOT(-2),
        ;
        public int value;

        WaitingExOpType(int value) {
            this.value = value;
        }
    }

}
