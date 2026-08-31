package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * 3A炸
 */
public class Type_Bomb3A extends NJPDKAbsType {

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        //炸弹
        boolean is3A = cardList.containsAll(ACECard);
        return is3A || cardList.size() == ACECard.size();
    }

    //3A炸可以压所有的牌型
    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        if (parameter.getCloneCardList().containsAll(ACECard)) { //3A炸最大
            return new ArrayList<>(ACECard);
        }
        return new ArrayList<>();
    }
}
