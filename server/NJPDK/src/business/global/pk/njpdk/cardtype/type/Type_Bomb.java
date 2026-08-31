package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import business.njpdk.c2s.cclass.NJPDK_define;

import java.util.ArrayList;
import java.util.List;

/**
 * 四炸
 */
public class Type_Bomb extends NJPDKAbsType {

    @Override
    public boolean checkCard(ArrayList<Integer> cardList) {
        return checkNormalType(cardList, 4, 1);
    }

    @Override
    public List<Integer> generateCardList(NJPDKALGParameter parameter) {
        ArrayList<Integer> cardList = parameter.getCloneCardList();
        int previousOpType = parameter.previousOpType;
        ArrayList<Integer> previousCardList = parameter.previousCardList;
        int previousMaxCard = 0;
        //压除3A炸的所有的牌型
        if (!algContainer.is3A(previousOpType, previousCardList)) {//前家不是3A炸或自己回合才能出四炸
            if (previousOpType == NJPDK_define.NJPDK_CARD_TYPE.PDK_CARD_TYPE_ZHADAN.value()) {
                previousMaxCard = algContainer.getMaxCard(previousCardList, 4);//获取上轮四炸
            }
            return algContainer.getBombByCardList(cardList, previousMaxCard).bodyList;
        }
        return new ArrayList<>();
    }

}
