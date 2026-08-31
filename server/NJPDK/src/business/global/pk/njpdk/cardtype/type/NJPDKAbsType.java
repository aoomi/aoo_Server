package business.global.pk.njpdk.cardtype.type;

import business.global.pk.njpdk.cardtype.NJPDKALGContainer;
import business.global.pk.njpdk.cardtype.NJPDKALGParameter;
import jsproto.c2s.cclass.pk.BasePocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 安岳抽象牌型模版
 */
public abstract class NJPDKAbsType {

    /**
     * 最大牌值
     */
    protected final int maxCardValue = 100;
    /**
     * 3A炸
     */
    protected List<Integer> ACECard = Arrays.asList(0x1E, 0x2E, 0x3E);
    /**
     * 算法容器
     */
    protected NJPDKALGContainer algContainer = NJPDKALGContainer.getInstance();

    /**
     * 检测牌型
     *
     * @param cardList
     * @return
     */
    public abstract boolean checkCard(ArrayList<Integer> cardList);

    /**
     * 按一定规则生成所需的牌型
     *
     * @param cardType
     * @return
     */
    public abstract List<Integer> generateCardList(NJPDKALGParameter cardType);

    /**
     * 获取牌值
     *
     * @param card
     * @return
     */
    public int getCardValue(int card) {
        return BasePocker.getCardValue(card);
    }

    /**
     * 检测最普通的牌型（牌全部相等的），对子，炸弹
     *
     * @param cardList
     * @param length
     * @param number
     * @return
     */
    protected boolean checkNormalType(ArrayList<Integer> cardList, int length, int number) {
        if (cardList.size() == length) {
            Map<Integer, Long> valueCountMap = algContainer.getValueCountMapByList(cardList);
            return valueCountMap != null && valueCountMap.size() == number;
        }
        return false;
    }

}
