package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 飞机带
 */
public class Type_Plane extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value(),
            NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI31.value(),
            NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //要保证最后一手
        //飞机带
        TargetCardContainer cc = algContainer.getPlaneByCardList(cardList, 0, maxCardValue, -1);
        return cc.planeCompareValue > 0;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        TargetCardContainer cc = new TargetCardContainer();
        int planeCompareValue = 0;
        int planeLength = -1;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            TargetCardContainer c1 = null;
            //压飞机带两张
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI33.value()) {
                c1 = algContainer.getPlaneWithTwoByCardList(previousCardList, planeCompareValue, maxCardValue, planeLength);
            }
            //压飞机带一张
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI31.value()) {
                c1 = algContainer.getPlaneWithAByCardList(previousCardList, planeCompareValue, maxCardValue, planeLength);
            }
            //压飞机带一对
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()) {
                c1 = algContainer.getPlaneWithPairsByCardList(previousCardList, planeCompareValue, maxCardValue, planeLength);
            }
            if (c1 != null) {
                planeCompareValue = c1.planeCompareValue;
                planeLength = c1.planeLength;
            }
            cc = algContainer.getPlaneByCardList(cardList, planeCompareValue, parameter.leftCardSize, planeLength);
            parameter.outTailNumber = 0;
        }
        return cc.bodyList;
    }

}
