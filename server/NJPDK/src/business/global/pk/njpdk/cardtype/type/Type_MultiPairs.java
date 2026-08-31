package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.List;

/**
 * 二对 Two pairs 4455（或者联队）
 */
public class Type_MultiPairs extends NJPDKAbsType {

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //联队
        return algContainer.checkStraight(cardList, 2);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        ArrayList<Integer> cardList = parameter.getCloneCardList();
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> previousCardList = parameter.previousCardList;
        //压开局,从大到小选择顺子
        if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value()) {
            for (int i = 8; i >= parameter.minStraightNumber; i--) {
                List<Integer> outList = algContainer.getMultiPairsByCardList(cardList, i, 0);
                if (outList.size() > 2) {
                    return outList;
                }
            }
        }
        //压联队
        if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_WANFA_LIANDUI.value()) {
            int previousMaxCard = algContainer.getMaxCard(previousCardList, 2);
            return algContainer.getMultiPairsByCardList(cardList, previousCardList.size() / 2, previousMaxCard);
        }
        return new ArrayList<>();
    }
}
