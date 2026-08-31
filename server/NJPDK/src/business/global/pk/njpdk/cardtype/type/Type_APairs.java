package business.global.pk.njpdk.cardtype.type;


import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 单对 A pairs（44）
 */
public class Type_APairs extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        return checkNormalType(cardList, 2, 1);
    }

    /**
     * 智能获取对子（对子>对子>开局）
     *
     * @return
     */
    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        if (cardTypeList.contains(previousOpType)) {//前家对子或自己回合才能出对子
            int previousMaxCard = 0;
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_DUIZI.value()) {
                previousMaxCard = getCardValue(parameter.previousCardList.get(0));
            }
            return algContainer.getAPairsByCardList(parameter.getCloneCardList(), previousMaxCard).bodyList;
        }
        return new ArrayList<>();
    }

}
