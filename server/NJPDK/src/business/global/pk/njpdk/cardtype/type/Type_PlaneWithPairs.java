package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 飞机一对
 */
public class Type_PlaneWithPairs extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //飞机带一对
        TargetCardContainer cc = algContainer.getPlaneWithPairsByCardList(cardList, 0, maxCardValue, -1);
        return cc.planeCompareValue > 0;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        ArrayList<Integer> cardList = parameter.getCloneCardList();
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> previousCardList = parameter.previousCardList;

        int planeCompareValue = 0;
        int planeLength = -1;
        if (cardTypeList.contains(previousOpType)) {
            //压飞机带1对
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_FEIJI32.value()) {
                TargetCardContainer c1 = algContainer.getPlaneWithPairsByCardList(previousCardList, 0, maxCardValue, -1);
                planeCompareValue = c1.planeCompareValue;
                planeLength = c1.planeLength;
            }
            TargetCardContainer cc = algContainer.getPlaneWithPairsByCardList(cardList, planeCompareValue, parameter.leftCardSize, planeLength);
            //剩余的牌如果小于等于牌
            if (cc.planeLength > 0) {
                //全出
                if (parameter.leftCardSize < cc.planeLength * 3 + cc.planeLength * 2) {
                    ArrayList<Integer> temp = parameter.getCloneCardList();
                    temp.removeAll(cc.bodyList);
                    cc.tailList = temp;
                } else if (cc.tailList.size() != cc.planeLength * 2) {
                    return new ArrayList();
                }
                parameter.outTailNumber = cc.tailList.size();
                cc.bodyList.addAll(cc.tailList);
                return cc.bodyList;
            }
        }
        return new ArrayList<>();
    }
}
