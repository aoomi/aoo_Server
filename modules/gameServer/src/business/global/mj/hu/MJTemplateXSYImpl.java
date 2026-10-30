package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.AbsTing;
import cenum.mj.OpPointEnum;

/**
 * 小三元：中发白 其中一个是将 两外两个为刻或者将
 */
public class MJTemplateXSYImpl extends BaseHuCard {

    /**
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return OpPointEnum.Not;
        }
        if (checkSanYuan(mSetPos, mCardInit, false)) {
            return OpPointEnum.XiaoSanYuan;
        }
        return OpPointEnum.Not;
    }

    /**
     * 检查三元
     *
     * @param mSetPos
     * @param mCardInit
     * @param daSanYuan
     * @return
     */
    public boolean checkSanYuan(AbsMJSetPos mSetPos, MJCardInit mCardInit, boolean daSanYuan) {
        int sizeJin = 0;
        long count;
        //找到中发白
        MJCardInit newMJInit = new MJCardInit(mCardInit.getAllCardInts(), mCardInit.sizeJin());
        for (int i = 45; i <= 47; i++) {
            int finalI = i;
            count = mCardInit.getAllCardInts().stream().filter(k -> k == finalI).count();
            if (count < 3) {
                //手牌么有 去公共牌找
                if (!mSetPos.publicCardTypeList().stream().anyMatch(k -> k / 100 == finalI)) {
                    //公共牌没有找金替代
                    sizeJin = Math.toIntExact(!daSanYuan ? 2 : 3 - count);
                    if (!checkJinTiZfb(mSetPos, newMJInit, sizeJin, finalI)) {
                        return false;
                    }
                    daSanYuan = true;
                }
            }
        }
        return true;
    }

    /**
     * 检查金替代中发白胡牌
     *
     * @param mSetPos
     * @param newMJInit
     * @param size
     * @param finalI
     * @return
     */
    public boolean checkJinTiZfb(AbsMJSetPos mSetPos, MJCardInit newMJInit, int size, int finalI) {
        if (size == 0) {//不用替换
            return true;
        }
        if (newMJInit.sizeJin() < size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            newMJInit.getAllCardInts().add(finalI);
            newMJInit.getJins().remove(0);
        }
        return checkHu(mSetPos, newMJInit);

    }

    /**
     * 检查换牌后是否还是胡的
     *
     * @param mSetPos
     * @param mCardInit
     */
    public boolean checkHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return ((AbsTing) MJFactory.getTingCard(mSetPos.getmActMrg())).tingHu(mSetPos, mCardInit);
    }


}
