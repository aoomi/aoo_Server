package cenum.mj;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MJCEnum {
    public static final List<Integer> WA_LONG = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
    public static final List<Integer> TI_LONG = Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29);
    public static final List<Integer> TO_LONG = Arrays.asList(31, 32, 33, 34, 35, 36, 37, 38, 39);

    public final static Map<Integer, List<Integer>> LongMap = new ConcurrentHashMap<>();

    static {
        LongMap.put(1, Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19));
        LongMap.put(2, Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29));
        LongMap.put(3, Arrays.asList(31, 32, 33, 34, 35, 36, 37, 38, 39));
    }

    private final static HashMap<Integer, List<Integer>> MaPaiMap = new HashMap<>();

    static {
        // 系数牌1、5、9，东风、红中，对应庄家位置；
        MaPaiMap.put(0, Arrays.asList(1, 5, 9, 41, 45));
        // 系数牌2、6，南风，发财，对应庄家下家位置；
        MaPaiMap.put(1, Arrays.asList(2, 6, 42, 46));
        // 系数牌3、7，西风，白板，对应庄家对家位置；
        MaPaiMap.put(2, Arrays.asList(3, 7, 43, 47));
        // 系数牌4、8，北风，对应庄家上家位置；
        MaPaiMap.put(3, Arrays.asList(4, 8, 44));
    }

    /**
     * 获取指定马牌系数列表
     *
     * @return
     */
    public final static List<Integer> MaPaiMap(int posID) {
        return MaPaiMap.get(posID);
    }

    /**
     * 中码159
     */
    public final static List<Integer> ZhongMaList = new ArrayList<>(Arrays.asList(1, 5, 9));

    /**
     * 十三幺，查牌 0：不能查牌，1：提示查牌，2：玩家选择查牌，3：玩家取消查牌
     *
     * @author Huaxing
     */
    public enum SSYSeeState {
        // 不能查牌
        No(0),
        // 提示查牌
        Allow(1),
        // 玩家明查牌
        See(2),
        // 玩家暗查牌
        AnSee(3),
        ;
        private int value;

        private SSYSeeState(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SSYSeeState valueOf(int value) {
            for (SSYSeeState flow : SSYSeeState.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return SSYSeeState.No;
        }
    }

    // 特殊胡法操作
    public enum OpHuEnum {
        Not(0), Not_Jin(1), // 没金
        Cai_Gui(2), // 财归（金归位）
        Jin(3),
        ;
        private int value;

        private OpHuEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }


    /**
     * 操作的胡类型
     *
     * @param opType
     * @return
     */
    public static HuType OpHuType(OpType opType) {
        switch (opType) {
            case Hu:
            case SanJinDao:
            case JingDiao:
            case TianHu:
            case YaoGangHu:
            case JiaHu:
                return HuType.ZiMo;
            case QiangJin:
                return HuType.QiangJin;
            case JiePao:
                return HuType.JiePao;
            case QiangGangHu:
                return HuType.QGH;
            case SiJinDao:
                return HuType.FHZ;
            case DDHu:
                return HuType.DDHu;
            case GSP:
                return HuType.GSP;
            case GSKH:
                return HuType.GSKH;
            case QiangTiHu:
                return HuType.QiangTiHu;
            default:
                break;
        }
        return HuType.NotHu;
    }


}
