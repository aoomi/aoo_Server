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
public class Type_FourZoneWithThree extends NJPDKAbsType {

    private List<Integer> cardTypeList = new ArrayList<>(Arrays.asList(NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value(), NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI3.value()));

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        ArrayList<Integer> lastCardList = (ArrayList<Integer>) cardList.clone();
        //3带2
        return algContainer.checkNormalType(lastCardList, 5, 1, 2, 0);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        int previousOpType = parameter.previousOpType;
        if (cardTypeList.contains(previousOpType)) {
            ArrayList<Integer> cardList = parameter.getCloneCardList();
            ArrayList<Integer> previousCardList = parameter.previousCardList;
            int previousMaxCard = 0;
            //压3带2
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_4DAI2.value()) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 4);
            }
            TargetCardContainer cc = algContainer.getBombByCardList(cardList, previousMaxCard);
            if (cc.bodyList.size() == 4) {
                ArrayList<Integer> temp = new ArrayList<>(cardList);
                temp.removeAll(cc.bodyList);
                cc.bodyList.addAll(temp);
                if (cc.bodyList.size() == 7) {
                    return cc.bodyList;
                }
            }
        }
        return new ArrayList<>();
    }
}
