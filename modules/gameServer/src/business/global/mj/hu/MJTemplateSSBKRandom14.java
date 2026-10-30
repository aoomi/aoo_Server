package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.hu.abs.AbsSSBK;
import cenum.mj.OpPointEnum;
import com.ddm.server.common.utils.CommMath;

import java.util.List;

/**
 * 1.定义1：不同花色的147、258或369，再加上七张字牌，这十六张中的任意不同十四张；
 * 三种花色都有；
 * 2.七星十三烂（七字全）：手牌有“东南西北中发白”有且只有一张的十三浪；
 */
public class MJTemplateSSBKRandom14 extends AbsSSBK {
    /**
     * @param mSetPos
     * @param mCardInit
     * @return
     */
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        //存在吃碰杠
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        return checkSSBK(mCardInit, mSetPos);
    }

    /**
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (checkHuCard(mSetPos, mCardInit)) {
            //七星十三烂（七字全）：手牌有“东南西北中发白”有且只有一张的十三浪；
            if (mCardInit.getAllCardInts().stream().filter(k -> k >= 41 && k <= 47).count() == 7) {
                return OpPointEnum.QXSSL;
            }
            return OpPointEnum.SSL;
        }
        return OpPointEnum.Not;
    }

    /**
     * 检查是否都是唯一风牌
     *
     * @param cardList
     * @param totalJin
     * @return
     */
    @Override
    public boolean checkFeng(List<Integer> cardList, int totalJin) {
        if (CommMath.hasSame(cardList)) {
            return false;
        }
        if (cardList.stream().anyMatch(k -> k > 47 || k < 40)) {
            return false;
        }
        return true;
    }

    /**
     * 检查牌间距
     *
     * @param cardList
     * @return
     */
    @Override
    public boolean checkCard(List<Integer> cardList) {
        CommMath.getSort(cardList, false);
        for (int i = 0, sizeI = cardList.size(); i < sizeI; i++) {
            for (int j = i + 1, sizeJ = cardList.size(); j < sizeJ; j++) {
                if (checkNotInSpace(cardList.get(i), cardList.get(j))) {
                    return false;
                }
                break;
            }
        }
        return true;
    }

    /**
     * 检查间距
     *
     * @param large
     * @param min
     * @return
     */
    public boolean checkNotInSpace(Integer large, Integer min) {
        //3筒  跟 九筒也可以
        return large / 10 == min / 10 && (large - min != 3 && large - min != 6);
    }
}
