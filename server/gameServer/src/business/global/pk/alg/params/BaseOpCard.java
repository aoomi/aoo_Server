package business.global.pk.alg.params;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 输出类型
 */
public class BaseOpCard extends BaseSendMsg {

    public int opCardType;
    public List<Integer> cardList = new ArrayList<>();
    public List<Integer> substituteCard = new ArrayList<>();
    public int daiNum;
    public int tripleNum;//3带个数
    public int compareValue;//对比值
    public int weight = 0;//权重

    public static BaseOpCard make(int opCardType, int daiNum, List<Integer> cardList) {
        BaseOpCard ret = new BaseOpCard();
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.daiNum = daiNum;
        return ret;
    }

    public static BaseOpCard make(int opCardType, int daiNum, List<Integer> cardList, int compareValue) {
        BaseOpCard ret = new BaseOpCard();
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.daiNum = daiNum;
        ret.compareValue = compareValue;
        return ret;
    }

    public static BaseOpCard make(int opCardType, int daiNum, List<Integer> cardList, int compareValue, int tripleNum) {
        BaseOpCard ret = new BaseOpCard();
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.daiNum = daiNum;
        ret.tripleNum = tripleNum;
        ret.compareValue = compareValue;
        return ret;
    }

    @Override
    public String toString() {
        return "BaseOpCard{" +
                "opCardType=" + opCardType +
                ", cardList=" + cardList +
                ", daiNum=" + daiNum +
                ", tripleNum=" + tripleNum +
                ", compareValue=" + compareValue +
                ", weight=" + weight +
                '}';
    }
}
