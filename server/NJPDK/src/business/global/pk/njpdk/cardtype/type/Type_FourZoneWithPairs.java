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
public class Type_FourZoneWithPairs extends NJPDKAbsType {
    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI21.value()));

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
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI21.value()) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 4);
            }
            TargetCardContainer cc = algContainer.getBombByCardList(cardList, previousMaxCard);
            if (cc.bodyList.size() == 4) {
                ArrayList<Integer> temp = new ArrayList<>(cardList);
                temp.removeAll(cc.bodyList);
                TargetCardContainer cc1 = algContainer.getAPairsByCardList(temp, 0);
                if (cc1.bodyList.size() == 2) {
                    cc.bodyList.addAll(cc1.bodyList);
                    return cc.bodyList;
                }
            }
        }
        return new ArrayList<>();
    }

}
