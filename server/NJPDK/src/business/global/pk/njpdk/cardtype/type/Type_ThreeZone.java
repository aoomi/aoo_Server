package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.global.pk.njpdk.cardtype.TargetCardContainer;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 3不带
 */
public class Type_ThreeZone extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(),
            NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value(),
            NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        ArrayList<Integer> previousCardList = (ArrayList<Integer>) cardList.clone();
        //3不带
        return algContainer.checkNormalType(previousCardList, 3, 1, 0, 0);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> cardList = parameter.getCloneCardList();
        if (parameter.leftCardSize != 3) {
            return new ArrayList<>();
        }
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            int previousMaxCard = 0;
            //压3带其他,而且只能最后一手
            List<Integer> typeList = Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI1.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI2.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3DAI21.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_3BUDAI.value());
            if (typeList.contains(previousOpType)) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 3);
            }
            TargetCardContainer cc = algContainer.getThreeZoneByCardList(cardList, previousMaxCard);
            if (cc.bodyList.size() == 3) {
                return cc.bodyList;
            }
        }
        return new ArrayList<>();
    }
}
