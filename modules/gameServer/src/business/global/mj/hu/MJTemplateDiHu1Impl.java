package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.HuType;
import cenum.mj.OpPointEnum;

import java.util.Map;

/**
 * 地胡：定义1、2可复选
 * 定义1：庄家出一张牌就点炮；
 */
public class MJTemplateDiHu1Impl extends BaseHuCard {
    /**
     * 定义1：庄家出一张牌就点炮；
     *
     * @param mSetPos
     * @return
     */
    public boolean checkDiHu1(AbsMJSetPos mSetPos) {
        if (mSetPos.getHuType().equals(HuType.ZiMo) || mSetPos.getOutCardIDs().size() > 0) {
            return false;
        }
        if (mSetPos.getSet().getDPos() == mSetPos.getPosID() || (mSetPos.getSet().getLastOpInfo() != null
                && mSetPos.getSet().getLastOpInfo().getLastOpPos() != mSetPos.getSet().getDPos())) {
            return false;
        }
        AbsMJSetPos dpos = mSetPos.getMJSetPos(mSetPos.getSet().getDPos());
        if (dpos.sizeOutCardIDs() - dpos.getPosOpRecord().sizeHua() != 1) {
            return false;
        }
        Map<Integer, AbsMJSetPos> posDict = mSetPos.getSet().getPosDict();
        for (AbsMJSetPos pos : posDict.values()) {
            if (pos.getPublicCardList().size() > 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * 本方法启动前提是已经胡了
     *
     * @param mSetPos
     * @param mCardInit
     * @param <T>
     * @return
     */
    @Override
    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (checkDiHu1(mSetPos)) {
            return OpPointEnum.DiHu;
        }
        return OpPointEnum.Not;
    }

    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return OpPointEnum.DiHu.equals(checkHuCardReturn(mSetPos, mCardInit));
    }
}

