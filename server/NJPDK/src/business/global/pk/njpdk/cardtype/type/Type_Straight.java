package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;
import jsproto.c2s.cclass.pk.BasePockerLogic;

import java.util.ArrayList;
import java.util.List;

/**
 * 顺子 straight（34567）
 */
public class Type_Straight extends NJPDKAbsType {

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        ArrayList<Integer> lastCardList = (ArrayList<Integer>) cardList.clone();
        //顺子
        return algContainer.checkStraight(lastCardList, 1);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        ArrayList<Integer> cardList = parameter.getCloneCardList();
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> previousCardList = parameter.previousCardList;
        //压开局
        if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_NOMARL.value()) {
            for (int i = 11; i >= 5; i--) {
                List<Integer> straight = algContainer.getStraightByCardList(cardList, i, 0);
                if (straight.size() >= 5) {
                    return straight;
                }
            }
        }
        //压顺子
        if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_SHUNZI.value()) {
            previousCardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
            return algContainer.getStraightByCardList(cardList, previousCardList.size(), getCardValue(previousCardList.get(0)));
        }
        return new ArrayList<>();
    }

    /**
     * 是不是牌型中有顺子
     *
     * @param cardList
     * @return
     */
    public boolean checkHaveStraight(ArrayList<Integer> cardList) {
        ArrayList<Integer> previousCardList = (ArrayList<Integer>) cardList.clone();
        previousCardList.sort(BasePockerLogic.sorterBigToSmallNotTrump);
        return algContainer.getStraightByCardList(previousCardList, previousCardList.size(), 0).size() >= 5;
    }
}
