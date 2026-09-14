package business.global.mj.extbussiness.hutype;


import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.MJSetPos;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.hu.abs.AbsSSBK;
import com.ddm.server.common.utils.CommMath;

import java.util.List;


/**
 * 麻将十三烂：东、南、西、北、中、发、白任意五张各有一张，加其余至少相隔两张的牌。平胡可6张风牌；
 *
 * @author Huaxing
 */
public class StandardMJSSBKImpl extends AbsSSBK {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (mSetPos.sizePublicCardList() > 0) {
            return false;
        }
        // 检查十三烂
        if (this.checkSSBK(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
            return true;
        }
        return false;
    }

    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        StandardMJPointItem item = null;
        if (checkHuCard(mSetPos, mCardInit)) {
            item = new StandardMJPointItem();
//            item.addOpPointEnum(OpPoint.SSL.name(), OpPoint.SSL.value());
        }
        return item;
    }


    /**
     * 检查风牌
     *
     * @param cardList
     * @param totalJin
     * @return
     */
    public boolean checkFeng(List<Integer> cardList, int totalJin) {
        if (!CommMath.notHasSame(cardList))
            return false;
        int sizeCard = cardList.size();
        if (sizeCard == 5 || sizeCard == 6) {
            return true;
        } else if (sizeCard > 6) {
            return false;
        }
        for (int i = 1; i <= totalJin; i++) {
            int sCard = sizeCard + i;
            if (sCard == 5 || sCard == 6) {
                return true;
            }
        }
        return false;

    }


    /**
     * 获取玩家牌型数据和金数量
     *
     * @param mSetPos     玩家
     * @param allCardList 手上牌
     * @param cardType    头牌
     * @param isJin       是否金
     * @return
     */
    public MJCardInit mCardInit(MJSetPos mSetPos, List<MJCard> allCardList,
                                int cardType, boolean isJin) {
        MJCardInit mCardInit = mSetPos.mjCardInit(allCardList, isJin);
        if (null == mCardInit)
            return mCardInit;
        if (cardType > 0) {
            if (isJin) {
                if (mSetPos.getSet().getmJinCardInfo().getJinKeys().contains(cardType)) {
                    mCardInit.addJins(cardType);
                } else {
                    mCardInit.addCardInts(cardType);
                }
            } else {
                mCardInit.addCardInts(cardType);
            }
        }

        return mCardInit;
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
                if (cardList.get(i) / 10 != cardList.get(j) / 10) { // 不是同一色的牌
                    break;
                }
                if (cardList.get(i) - cardList.get(j) < 3) { // 同一色的牌,至少相隔两张的牌
                    return false;
                }
                break;
            }
        }
        return true;
    }


}
