package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 3带一对
 */
public class Type_ThreeZoneWithPairs extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        ArrayList<Integer> cloneCardList = (ArrayList<Integer>) cardList.clone();
        //3带2一对
        return algContainer.checkNormalType(cloneCardList, 3, 1, 2, 1);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            int previousMaxCard = 0;
            //压3带2一对
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value()) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 3);
            }
            TargetCardContainer cc = algContainer.getThreeZoneWithPairsByCardList(cardList, previousMaxCard);
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
