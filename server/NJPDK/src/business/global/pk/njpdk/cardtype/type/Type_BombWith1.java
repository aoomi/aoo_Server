package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 4带1炸弹
 */
public class Type_BombWith1 extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI1.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        if (cardList.size() == 5) {
            Map<Integer, Long> valueCountMap = algContainer.getValueCountMapByList(cardList);
            if (valueCountMap != null && valueCountMap.size() == 2 && valueCountMap.values().contains(4)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> previousCardList = parameter.previousCardList;
        int previousMaxCard = 0;
        //压除3A炸的所有的牌型
        if (!algContainer.is3A(previousOpType, previousCardList)) {
            if (cardTypeList.contains(previousOpType)) {
                if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI1.value()) {
                    previousMaxCard = algContainer.getMaxCard(previousCardList, 4);
                }
                List<Integer> out = getBombWith1ByCardList(parameter.getCloneCardList(), previousMaxCard, parameter);
                return out;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 获取炸弹
     *
     * @param cardList
     * @param lastMaxCard
     * @param parameter
     * @return
     */
    private List<Integer> getBombWith1ByCardList(ArrayList<Integer> cardList, int lastMaxCard, NJPDKALGParameter parameter) {
        TargetCardContainer cc = algContainer.getBombWith1ByCardList(cardList, lastMaxCard);
        cc.bodyList.addAll(cc.tailList);
        if (cc.bodyList.size() == 5 || (parameter.leftCardSize == cc.bodyList.size() && cc.bodyList.size() == 4)) {
            return cc.bodyList;
        }
        return new ArrayList<>();
    }

}
