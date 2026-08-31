package business.njpdk.c2s.cclass;

/*
 * 资阳跑得快宏定义
 * @author zaf
 * **/
public class NJPDK_define {

    //安岳跑得快每局游戏状态
    public enum NJPDK_GameStatus {
        PDK_GAME_STATUS_SENDCARD(0), //发牌
        PDK_GAME_STATUS_COMPAER_SECOND(1), //比牌
        PDK_GAME_STATUS_RESULT(2), //结算
        ;

        private int value;

        NJPDK_GameStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static NJPDK_GameStatus valueOf(int value) {
            for (NJPDK_GameStatus flow : NJPDK_GameStatus.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return NJPDK_GameStatus.PDK_GAME_STATUS_SENDCARD;
        }
    }

    public enum NJPDKJieSanShu {
        Jie2(2), Jie3(3), Jie5(4);
        int value;

        private NJPDKJieSanShu(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static NJPDKJieSanShu valueOf(int value) {
            for (NJPDKJieSanShu huPai : NJPDKJieSanShu.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return NJPDKJieSanShu.Jie2;
        }
    }

    /**
     * 牌型玩法
     */
    public enum NJPDK_WANFA {
        PDK_WANFA_3BUDAIZUIHOU(0),            //3不带,最后一手
        PDK_WANFA_3DAI1(1),            //3带1
        PDK_WANFA_3DAI2(2),            //3带2一对 33344
        PDK_CARD_TYPE_3DAI21(3),        //3带2
        PDK_WANFA_LIANDUI2(4),            //姐妹对，连队
        PDK_CARD_TYPE_4DAI1(5),            //4带1
        PDK_CARD_TYPE_3A(6),            //3A
        PDK_CARD_TYPE_4DAI21(7),        //4带2
        PDK_CARD_TYPE_4DAI3(8),        //4带3
        PDK_WANFA_3BUDAI(9),            //3不带,最后一手
        ;
        private int value;

        NJPDK_WANFA(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static NJPDK_WANFA getGameType(String value) {
            String gameTypyName = value.toUpperCase();
            for (NJPDK_WANFA flow : NJPDK_WANFA.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return null;
        }

        public static NJPDK_WANFA valueOf(int value) {
            for (NJPDK_WANFA flow : NJPDK_WANFA.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return null;
        }
    }

    //牌类型
    public enum NJPDK_CARD_TYPE {
        PDK_CARD_TYPE_NOMARL(0),            //默认状态，回合开始默认阶段
        PDK_CARD_TYPE_BUCHU(1),                //不出
        PDK_WANFA_SINGLECARD(2),            //单牌
        PDK_CARD_TYPE_DUIZI(3),            //对子
        PDK_CARD_TYPE_SHUNZI(4),            //顺子
        PDK_CARD_TYPE_3BUDAI(5),            //3不带（最后一手）
        PDK_CARD_TYPE_3DAI1(6),            //3带1
        PDK_CARD_TYPE_3DAI2(7),            //3带2
        PDK_CARD_TYPE_4DAI1(8),            //4带1
        PDK_CARD_TYPE_4DAI2(9),            //4带2
        PDK_CARD_TYPE_4DAI3(10),            //4带3
        PDK_CARD_TYPE_ZHADAN(11),            //炸弹
        PDK_CARD_TYPE_FEIJI3(12),            //飞机3
        PDK_CARD_TYPE_FEIJI4(13),            //飞机4
        PDK_WANFA_LIANDUI(14),                //联队
        PDK_CARD_TYPE_3DAI21(15),            //3带2一对
        PDK_CARD_TYPE_FEIJI31(16),            //飞机带一张
        PDK_CARD_TYPE_FEIJI32(17),            //飞机带一对
        PDK_CARD_TYPE_FEIJI33(18),            //飞机带两张
        PDK_CARD_TYPE_FEIJI34(19),            //飞机带不带
        PDK_CARD_TYPE_4DAI21(20),            //4带2一对
        ;
        private int value;

        NJPDK_CARD_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static NJPDK_CARD_TYPE getGameType(String value) {
            String gameTypeName = value.toUpperCase();
            for (NJPDK_CARD_TYPE flow : NJPDK_CARD_TYPE.values()) {
                if (flow.toString().equals(gameTypeName)) {
                    return flow;
                }
            }
            return NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI;
        }

        public static NJPDK_CARD_TYPE valueOf(int value) {
            for (NJPDK_CARD_TYPE flow : NJPDK_CARD_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI;
        }
    }

    /**
     * 炸弹分数计算
     */
    public enum BombScore {
        DOUBLE_(0),            // 翻倍,4倍截止
        ADD_NOT(1),            // 炸弹不翻倍
        ADD_TEN(2),            // 加10分
        ADD_FIVE(3);            // 加5分


        private int type;

        BombScore(int type) {
            this.type = type;
        }

        /**
         * 类型转枚举
         *
         * @param type
         * @return
         */
        public static BombScore valueOf(int type) {
            for (BombScore bombScore : BombScore.values()) {
                if (bombScore.type == type)
                    return bombScore;
            }
            throw new IllegalArgumentException("炸弹分数计算");
        }

        /**
         * 是否相同炸弹计算方式
         *
         * @param type
         * @return
         */
        public boolean has(int type) {
            return this.type == type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * 出牌玩家
     */
    public enum FirstCardPosType {
        Has_Spade_Three_Of_FirstSet(0),    // 首局有黑桃三
        Has_Spade_Three_Of_EverySet(1),    // 每局拥有黑桃3的先出
        Random_EverySet(2);            // 每局随机选玩家

        private int type;

        FirstCardPosType(int type) {
            this.type = type;
        }

        /**
         * 首出玩家类型获取
         *
         * @param type
         * @return
         */
        public static FirstCardPosType valueOf(int type) {
            for (FirstCardPosType firstCardPosType : FirstCardPosType.values()) {
                if (firstCardPosType.type == type)
                    return firstCardPosType;
            }
            return null;
        }

        /**
         * 是否有该首出类型
         *
         * @param type
         * @return
         */
        public boolean has(int type) {
            return this.type == type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * 首出包含的牌
     */
    public enum FirstCardType {
        Has_Spade_Three(0),     // 带黑桃三
        Random_EverySet(1);     // 任意牌

        private int type;

        FirstCardType(int type) {
            this.type = type;
        }

        /**
         * 首出牌类型获取
         *
         * @param type
         * @return
         */
        public static FirstCardType valueOf(int type) {
            for (FirstCardType firstCardType : FirstCardType.values()) {
                if (firstCardType.type == type)
                    return firstCardType;
            }
            return null;
        }

        /**
         * 是否有该首出牌类型
         *
         * @param type
         * @return
         */
        public boolean has(int type) {
            return this.type == type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * 可选玩法
     */
    public enum KeXuanWanFa {
        JiPaiQi(0),     // 记牌器
        DisplayRemainingCard(1),     // 显示余牌
        TwoCardReport(2),     // 两张报对（放走包赔）
        ZiDong(3),     // 自动准备
        ChunTian(4),
        Card15(5);
        private int type;

        KeXuanWanFa(int type) {
            this.type = type;
        }

        public static KeXuanWanFa valueOf(int type) {
            for (KeXuanWanFa keXuanWanFa : KeXuanWanFa.values()) {
                if (keXuanWanFa.type == type)
                    return keXuanWanFa;
            }
            return null;
        }

        public boolean has(int type) {
            return this.type == type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * 不限制,30秒出牌,60秒出牌
     *
     * @author Administrator
     */
    public enum NJPDKXianShi {
        NOT(0), // 不限制
        SHi_15(15100), // 15秒出牌
        SHI_60(60100),// 60秒出牌
        SHI_120(120100),// 120秒出牌
        SHI_240(240100),// 240秒出牌
        SHI_360(360100),// 360秒出牌
        ;
        private int value;

        private NJPDKXianShi(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static NJPDKXianShi valueOf(int value) {
            for (NJPDKXianShi huPai : NJPDKXianShi.values()) {
                if (huPai.ordinal() == value) {
                    return huPai;
                }
            }
            return NJPDKXianShi.NOT;
        }
    }


}
