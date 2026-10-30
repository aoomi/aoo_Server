package business.global.mj.hu;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

/**
 * 十八罗汉：即杠了四次，剩下一张牌听牌胡牌；（需创建房间时选择开启才有此牌型）
 *
 * @author Huaxing
 */
public class LuoHan18Impl extends BaseHuCard {
    @Override
    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (null == mCardInit) {
            return false;
        }
        if (mSetPos.sizePublicCardList() <= 3) {
            return false;
        }
        if (mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Gang.value() || k.get(0) == OpType.JieGang.value() || k.get(0) == OpType.AnGang.value()).count() >= 4L) {
            Integer duiCard = mCardInit.getAllCardInts().get(0) % 10;
            return !CommMath.notHasSame(mCardInit.getAllCardInts()) && (duiCard == 2 || duiCard == 5 || duiCard == 8);
        } else {
            return false;
        }
    }
}
