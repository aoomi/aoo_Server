package business.global.mj.extbussiness;
/**
 * 麻将配置
 *
 * @author Administrator
 */
public class StandardMJRoomEnum {

    /**
     * 动作分数（不允许扩展，自己子类写）
     *
     * @author Administrator
     */
    public enum OpPoint {
        QSSL(4),//七星十三烂
        SSL(2),//十三烂
        GSKH(2),//杠上开花
        QiangGangHu(4),//抢杠胡
        TianHu(1),//天胡
        DiHu(1),//地胡
        JingDiao(2),//精吊
        DG(2),//德国
        PingHu(1),//平胡
        PPHu(2),//大七对
        QD(4),//七小对
        ZiMo(2),//自摸
        JiePao(2),//炮胡
        ZhuangJia(2),//庄家
        AnGang(1),//暗杠
        Gang(1),//补杠
        JieGang(1),//接杠
        MingGang(1),//明杠
        Piao(1),//飘分
        ChaoZhuang(1),//抄庄

        //临时存储
        GangPoint(0),//杠分
        HuPoint(0),//胡分
        ;
        private int value;

        private OpPoint(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * 这个根据自己需求写，不然就按这边
     */
    public enum GameRoomConfigEnum {
        /**
         * 房间内切换人数
         */
        FangJianQieHuanRenShu,
        /**
         * 自动准备
         */
        ZiDongZhunBei,
        /**
         * 小局托管解散
         */
        SetAutoJieSan,
        /**
         * 解散次数
         */
        JieSanCishu5,
        /**
         * 托管2小局解散
         */
        TuoGuan2XiaoJuJieSan,
        /**
         * 解散次数
         */
        JieSanCishu3,
        /**
         * 小局10秒自动准备
         */
        XiaoJu10Miao,
        ;

        /**
         * 获取下标
         *
         * @return int
         */
        public static int getOrdinalByName(String name) {
            for (GameRoomConfigEnum value : GameRoomConfigEnum.values()) {
                if (value.name().equals(name)) {
                    return value.ordinal();
                }
            }
            return -1;
        }
    }


    /**
     * 这个根据自己要求调用，开金的方式
     */
    public enum KaiJin {
        /**
         * 无金
         */
        NotJin(false,0),
        /**
         * 单金
         */
        SingleJin(false,1),
        /**
         * 双金
         */
        DoubleJin(false,2),
        /**
         * 进金+金
         */
        JinJin(false,3),
        /**
         * 单金扣掉翻开的那张
         */
        SingleJin1(true,1),
        /**
         * 双金扣掉翻开的那张
         */
        DoubleJin1(true,2),
        /**
         * 进金+金（扣掉翻开的那张）
         */
        JinJin1(true,3),
        ;

        //是否扣掉翻开的那张牌
        public boolean deductedThisCard;
        public int value;
        KaiJin(boolean deductedThisCard, int value){
            this.deductedThisCard = deductedThisCard;
            this.value = value;
        }
    }

    /**
     * 这个根据自己要求调用，换n张的方式
     */
    public enum HuanZhang {
        /**
         * 不换
         */
        NotHuan(false,0),
        /**
         * 同色换三张
         */
        HuanTongSe3(true,3),
        /**
         * 不同色换三张
         */
        HuanBuTongSe3(false,3),
        ;

        //同色
        public boolean tongSe;
        public int value;
        HuanZhang(boolean tongSe, int value){
            this.tongSe = tongSe;
            this.value = value;
        }

        public static HuanZhang getTHuanZhang(int ordinal){
            for (HuanZhang huanZhang : HuanZhang.values()) {
                if(huanZhang.ordinal() == ordinal){
                    return huanZhang;
                }
            }
            return HuanZhang.NotHuan;
        }
    }

    /**
     * 换张顺序 顺时针 逆时针 对家（4人场才有）,随机1对1
     */
    public enum HuanZhangOderBy {
        ShunShiZhen,
        NiShiZhen,
        DuiJia,
        SuiJi,
        ;
        public static HuanZhangOderBy valueOf(int odinal) {
            for (HuanZhangOderBy flow : HuanZhangOderBy.values()) {
                if (flow.ordinal() == odinal) {
                    return flow;
                }
            }
            return HuanZhangOderBy.ShunShiZhen;
        }
    }

    /**
     * 封顶
     */
    public enum FengDing {
        /**
         * 133
         */
        F133(133),
        /**
         * 256
         */
        F256(256),
        /**
         * 500
         */
        F500(500),
        /**
         * 不封顶
         */
        Not(0),
        ;

        int value;

        FengDing(int value) {
            this.value = value;
        }

        public static int getValue(int ordinal) {
            for (FengDing item : FengDing.values()) {
                if (item.ordinal() == ordinal) {
                    return item.value;
                }
            }
            return 0;
        }
    }

    /**
     * 锅分
     */
    public enum GuoFen {

        /**
         * 锅分
         */
        Not(0),
        /**
         * 50锅分
         */
        G50(50),
        /**
         * 100锅分
         */
        G100(100),
        /**
         * 300锅分
         */
        G300(300),
        ;

        int value;

        GuoFen(int value) {
            this.value = value;
        }

        public static int getValue(int ordinal) {
            for (GuoFen item : GuoFen.values()) {
                if (item.ordinal() == ordinal) {
                    return item.value;
                }
            }
            return 0;
        }
    }

    /**
     * 这个根据自己要求调用，买马个数的方式
     */
    public enum MaiMa {
        Zero(0,false),//不奖马
        Two(4,true),//4马
        Four(6,true),//6马
        Six(8,true),//8马
        Eight(10,true),//10马
        ;
        private int value;
        private boolean sameZhongMa;

        private MaiMa(int value,boolean sameZhongMa) {
            this.value = value;
            this.sameZhongMa = sameZhongMa;
        }

        public int value() {
            return value;
        }

        public boolean isSameZhongMa() {
            return sameZhongMa;
        }

        public static MaiMa valueOf(int value) {
            for (MaiMa flow : MaiMa.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return MaiMa.Zero;
        }
    }


    /**
     * 这个根据自己要求调用，漂分的方式
     */
    public enum Piao {
        /**
         * 固漂1
         */
        GP1(1),
        /**
         * 固漂2
         */
        GP2(2),
        /**
         * 固漂3
         */
        GP3(3),
        /**
         * 不票
         */
        Not(-1),
        /**
         * 每局漂
         */
        Every(0),
        ;

        //漂的值
        public int value;
        Piao(int value){
            this.value = value;
        }

        public static Piao getPiao(int ordinal){
            for (Piao piao : Piao.values()) {
                if(piao.ordinal() == ordinal){
                    return piao;
                }
            }
            return Piao.Not;
        }
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
}
