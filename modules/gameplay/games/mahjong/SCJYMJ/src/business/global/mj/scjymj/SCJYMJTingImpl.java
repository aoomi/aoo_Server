package business.global.mj.scjymj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.hu.NormalHuCardImpl;
import business.global.mj.manage.MJFactory;
import business.global.mj.scjymj.hutype.SCJYMJDDHuCardImpl;
import business.global.mj.ting.AbsTing;
import business.global.mj.util.HuUtil;
import cenum.mj.MJSpecialEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 江苏扬中麻将
 *
 * @author Huaxing
 */
public class SCJYMJTingImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (MJFactory.getHuCard(SCJYMJDDHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        if (MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }


    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    @Override
    public List<Integer> absCheckTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        SCJYMJSetPos aPos = (SCJYMJSetPos) mSetPos;
        List<Integer> ret = new ArrayList<>();
        // 遍历其他牌
        MJCardInit mInit = aPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        if (!aPos.getSet().getmJinCardInfo().checkExistJin()) {
            return ret;
        }

        boolean isHu = true;
        if (aPos.getSet().getmJinCardInfo().getJin(1).getType() != MJSpecialEnum.NOT_JIN.value()) {
            isHu = tingHu(aPos, new MJCardInit(mInit.getAllCardInts(), 0));

            if (!isHu) {
                return ret;
            }
            ret.addAll(aPos.getSet().getmJinCardInfo().getJinKeys());
        }

        // 遍历其他牌
        for (int type : HuUtil.CheckTypes) {
            isHu = tingHu(aPos, new MJCardInit(mInit.getAllCardInts(), type));
            if (isHu) {
                if (aPos.getSet().getmJinCardInfo().checkJinExist(type)) {
                    continue;
                } else {
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }

            }

        }
        mInit = null;
        allCardList = null;
        return ret;
    }


    /**
     * 检查杠后是否可以听胡
     *
     * @param mSetPos
     * @param mCardInit
     * @return
     */
    public boolean checkTingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (MJFactory.getHuCard(NormalHuCardImpl.class).checkHuCard(null, mCardInit)) {
            return true;
        }
        return false;
    }

    /**
     * 检查听到的牌
     *
     * @param mInit
     * @return
     */
    public boolean checkTingCardList(MJCardInit mInit) {
        List<Integer> ret = new ArrayList<>();
        boolean isHu = true;
        // 遍历其他牌
        for (int type : HuUtil.CheckTypes) {
            isHu = checkTingHu(null, new MJCardInit(mInit.getAllCardInts(), type));
            if (isHu) {
                if (!ret.contains(type)) {
                    return true;
                }
            }
        }
        mInit = null;
        return false;
    }
}
