package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.Iterator;
import java.util.Map;
import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;

public class TypeWeight {
    //最大权重
    public static final int maxWeight = 1000000;

    public static int getWeight(NJPDKALGParameter parameter) {
        int weight = 0;
        if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI34.value()) {
            weight += 500;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()) {
            weight += 800;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value()) {
            weight += 600;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI31.value()) {
            weight += 500;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_LIANDUI.value()) {
            weight += 300;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_SHUNZI.value()) {
            weight += 300;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value()) {
            weight += 140;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value()) {
            weight += 200;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value()) {
            weight += 180;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value()) {
            weight += 150;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
            weight += 100;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value()) {
            weight += 50;
        } else if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value()) {
            weight += 10;
        }
        weight += parameter.outCardList.size() * 5;//张数权重分
        if (parameter.outCardList.size() == parameter.getCardList().size()) {
            weight += maxWeight;
            if (parameter.outCardType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
                weight += maxWeight;
            }
        }
        parameter.weight = weight;
        return weight;
    }

    /**
     * 根据概率获取随机key
     *
     * @param totalWeight 总份额权重
     * @param chance      概率分布的map<key,份额权重>
     * @return
     */
    public static NJPDKALGParameter getRandomCard(int totalWeight, Map<NJPDKALGParameter, Integer> chance) {
        return getRandomCard(totalWeight, chance, SeededGameRandomSource.create());
    }

    public static NJPDKALGParameter getRandomCard(int totalWeight, Map<NJPDKALGParameter, Integer> chance, GameRandomSource random) {
        int randomNumber = random.nextInt(totalWeight);
        int min = 0;
        int max = 0;
        for (Iterator iter = chance.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<NJPDKALGParameter, Integer> entry = (Map.Entry<NJPDKALGParameter, Integer>) iter.next();
            max = max + entry.getValue();
            if (randomNumber >= min && randomNumber <= max) {
                return entry.getKey();
            }
            min = max;
        }
        return new NJPDKALGParameter();
    }
}
