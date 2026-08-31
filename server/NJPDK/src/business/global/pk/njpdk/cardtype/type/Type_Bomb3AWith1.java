package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * 3A加1炸
 */
public class Type_Bomb3AWith1 extends NJPDKAbsType {

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //炸弹
        boolean is3A = cardList.containsAll(ACECard);
        return is3A && cardList.size() == ACECard.size() + 1;
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        List<Integer> tailList = algContainer.getBomb3AWith1ByCardList(parameter.getCloneCardList());
        if (tailList.size() == 4 || (parameter.leftCardSize == tailList.size() && tailList.size() == 3)) {
            return tailList;
        }
        return new ArrayList<>();
    }
}
