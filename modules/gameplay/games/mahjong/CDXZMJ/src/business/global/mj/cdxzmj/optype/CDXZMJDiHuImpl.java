package business.global.mj.cdxzmj.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.cdxzmj.CDXZMJSetPos;
import business.global.mj.hu.BaseHuCard;
import cenum.mj.OpPointEnum;

/**
 * 地胡：定义1、2可复选
 * 定义1：庄家出一张牌就点炮；
 */
public class CDXZMJDiHuImpl extends BaseHuCard {
    /**
     * 定义1：闲家胡庄家打出的第一张牌（庄家起手杠后打的牌也算第一张）；
     *
     * @param mSetPos
     * @return
     */
    public boolean checkDiHu1(AbsMJSetPos mSetPos) {
        int dPos = mSetPos.getSet().getDPos();
        if (dPos == mSetPos.getPosID() || mSetPos.getOutCardIDs().size() > 0) {
            return false;
        }
        if ( mSetPos.sizeOutCardIDs() == 0) {
            return ((CDXZMJSetPos) mSetPos).isTing();
        }
        return false;
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
}	
	
