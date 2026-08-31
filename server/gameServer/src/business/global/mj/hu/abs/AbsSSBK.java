package business.global.mj.hu.abs;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.BaseHuCard;
import business.global.mj.util.HuUtil;
import com.ddm.server.common.utils.CommMath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 十三不靠
 *
 * @author Huaxing
 */
public abstract class AbsSSBK extends BaseHuCard {

    public abstract boolean checkFeng(List<Integer> cardList, int totalJin);

    protected boolean checkSSBK(MJCardInit mCardInit, AbsMJSetPos mSetPos) {
        HashMap<Integer, List<Integer>> BTJAllmap = HuUtil.getInstance().checkBTJAll(mCardInit.getAllCardInts(), mSetPos.getSet().getmJinCardInfo().getJinKeys());
        if (null == BTJAllmap) {
            if (checkSSBK(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
                return true;
            }
        } else {
            for (int i = 0; i < BTJAllmap.size(); i++) {
                if (checkSSBK(BTJAllmap.get(i), mCardInit.sizeJin())) {
                    return true;
                }
            }
        }

        return false;

    }

    /**
     * 检查用户是否胡
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    protected boolean checkSSBK(List<Integer> allCards, int totalJin) {
        Map<Boolean, List<Integer>> partitioned = allCards
                .stream().collect(Collectors.partitioningBy(e -> e >= 40));
        if (null == partitioned) {
            return false;
        }
        if (checkFeng(partitioned.get(true), totalJin)
                && checkCard(partitioned.get(false))) {
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
                if (cardList.get(i) - cardList.get(j) < 2) {
                    return false;
                }
                break;
            }
        }
        return true;
    }
}
