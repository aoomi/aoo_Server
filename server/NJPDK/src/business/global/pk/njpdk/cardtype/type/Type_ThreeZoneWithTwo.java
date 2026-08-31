package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The three-zone 三带二（44456）
 */
public class Type_ThreeZoneWithTwo extends NJPDKAbsType {

    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        ArrayList<Integer> lastCardList = (ArrayList<Integer>) cardList.clone();
        //3带2
        return algContainer.checkNormalType(lastCardList, 3, 1, 2, 0);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            int previousMaxCard = 0;
            //压3带2
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value()) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 3);
            }
            TargetCardContainer cc = algContainer.getThreeZoneWithTwoByCardList(cardList, previousMaxCard);
            if (cc.bodyList.size() == 3) {
                cc.bodyList.addAll(cc.tailList);
                if (cc.bodyList.size() == 5 || (parameter.leftCardSize < 5 && (cc.bodyList.size() == 4 || cc.bodyList.size() == 3))) {
                    return cc.bodyList;
                }
            }
        }
        return new ArrayList<>();
    }
}
