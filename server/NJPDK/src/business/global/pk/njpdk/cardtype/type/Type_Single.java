package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Type_Single extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //单张
        return cardList.size() == 1;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        int previousMaxCard = 0;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            //压单牌
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_SINGLECARD.value()) {
                previousMaxCard = getCardValue(previousCardList.get(0));
            }
            return algContainer.getSingleByCardList(cardList, previousMaxCard);
        }
        return new ArrayList<>();
    }
}
