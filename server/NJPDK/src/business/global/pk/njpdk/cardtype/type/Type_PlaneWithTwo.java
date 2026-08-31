package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 飞机带两张
 */
public class Type_PlaneWithTwo extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //飞机带2张
        TargetCardContainer cc = algContainer.getPlaneWithTwoByCardList(cardList, 0, maxCardValue, -1);
        return cc.planeCompareValue > 0;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        int planeCompareValue = 0;
        int planeLength = -1;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            //压飞机带2张
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value()) {
                TargetCardContainer c1 = algContainer.getPlaneWithTwoByCardList(previousCardList, planeCompareValue, maxCardValue, planeLength);
                planeCompareValue = c1.planeCompareValue;
                planeLength = c1.planeLength;
            }
            TargetCardContainer cc = algContainer.getPlaneWithTwoByCardList(cardList, planeCompareValue, parameter.leftCardSize, planeLength);
            parameter.outTailNumber = cc.tailList.size();
            cc.bodyList.addAll(cc.tailList);
            return cc.bodyList;
        }
        return new ArrayList<>();
    }
}
