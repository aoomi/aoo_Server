package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.MJCardCfg;
import com.ddm.server.common.utils.CommMath;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 烂胡：由序数间隔超过或等于2的同色牌加字牌组成的牌型，不能有将或者刻子，无需组成胡牌牌型，无需258做将；
 *
 * @author Huaxing
 */
public class LuanHuImpl extends BaseHuCard {

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        if (null == mCardInit) {
            return false;
        }
        // 检查十三烂
        return this.checkSSBK(mCardInit.getAllCardInts(), mCardInit.sizeJin());
    }

    /**
     * 检查用户是否胡
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    protected boolean checkSSBK(List<Integer> allCards, int totalJin) {
        if (((int) allCards.stream().distinct().count()) != allCards.size()) {
            return false;
        }
        Map<Boolean, List<Integer>> partitioned = allCards
                .stream().collect(Collectors.partitioningBy(e -> e >= 40));
        if (null == partitioned) {
            return false;
        }
        if (checkCard(partitioned.get(false))) {
            return true;
        }
        return false;
    }

    /**
     * 检查牌间距
     *
     * @param cardList
     * @return
     */
    @SuppressWarnings("unused")
    public boolean checkCard(List<Integer> cardList) {
        CommMath.getSort(cardList, false);
        for (int i = 0, sizeI = cardList.size(); i < sizeI; i++) {
            for (int j = i + 1, sizeJ = cardList.size(); j < sizeJ; j++) {
                if (hunYiSe(cardList.get(i) / 10) == hunYiSe(cardList.get(j) / 10) && cardList.get(i) - cardList.get(j) < 2) {
                    return false;
                }
                break;
            }
        }
        return true;
    }


    public int hunYiSe(int cardType) {
        if (MJCardCfg.WANG.value() == cardType) {
            return MJCardCfg.WANG.value();
        } else if (MJCardCfg.TIAO.value() == cardType) {
            return MJCardCfg.TIAO.value();
        } else if (MJCardCfg.TONG.value() == cardType) {
            return MJCardCfg.TONG.value();
        } else if (MJCardCfg.FENG.value() == cardType) {
            return MJCardCfg.FENG.value();
        } else {
            return MJCardCfg.NOT.value();
        }
    }

}
