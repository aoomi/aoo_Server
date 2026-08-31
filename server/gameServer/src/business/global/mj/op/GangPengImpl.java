package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.OpType;

import java.util.List;

/**
 * 碰后不能补杠
 *
 * @author Huaxing
 */
public class GangPengImpl implements OpCard {

    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID) {
        if (mSetPos.getSet().getLastOpInfo().checkBuGang(mSetPos.getHandCard().type, OpType.Not)) {
            // 该首牌不能继续补杠了。
            return false;
        }
        // 检查是否可以补杠
        int buGang = checkBuGang(mSetPos, mSetPos.getPublicCardList(),
                mSetPos.getHandCard().type, mSetPos.getHandCard().cardID);
        if (buGang > 0) {
            // 该牌型下次不能补杠了。
            mSetPos.getSet().getLastOpInfo().addBuGang(mSetPos.getHandCard().type, OpType.Not);
            return true;
        }
        return false;
    }

    /**
     * 补杠
     *
     * @param publicCardList 已经亮出的牌
     * @param type           牌类型
     * @param cardID         牌号
     * @return
     */
    private int checkBuGang(AbsMJSetPos mSetPos, List<List<Integer>> publicCardList, int type,
                            int cardID) {
        // 补杠类型如果是否金牌，则不能补杠
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            return 0;
        }
        // 如果没有公共牌。
        if (null == publicCardList || publicCardList.size() <= 0) {
            return 0;
        }

        List<Integer> prePublicCard = null;
        for (int i = 0; i < publicCardList.size(); i++) {
            prePublicCard = publicCardList.get(i);
            if (prePublicCard.get(0) == OpType.Peng.value()) {
                if (prePublicCard.get(2) / 100 == type) {
                    return cardID;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        if (doBuGang(mSetPos.getHandCard().type, mSetPos.getHandCard().cardID,
                mSetPos.getPublicCardList(), mSetPos)) {
            return true;
        }
        return false;
    }

    // 点 补杠
    private boolean doBuGang(int type, int cardID,
                             List<List<Integer>> publicCardList, AbsMJSetPos mSetPos) {
        // 搜集碰
        int gangCard = 0;
        List<Integer> prePublicCard = null;
        for (int i = 0; i < publicCardList.size(); i++) {
            prePublicCard = publicCardList.get(i);
            if (prePublicCard.get(0) == OpType.Peng.value()) {
                if (prePublicCard.get(2) / 100 == type) {
                    gangCard = cardID;
                    break;
                }
            }
        }
        if (null == prePublicCard) {
            return false;
        }

        // peng -》gang
        if (null != prePublicCard) {
            if (gangCard == cardID) {
                prePublicCard.set(0, OpType.Gang.value());
                prePublicCard.add(cardID);
            } else {
                return false;
            }
        }

        if (mSetPos.getHandCard().cardID == cardID) {
            // 清理手牌
            mSetPos.cleanHandCard();
        } else {
            mSetPos.removePrivateCard(new MJCard(cardID));
            mSetPos.addPrivateCard(mSetPos.getHandCard());
            mSetPos.sortCards();
            mSetPos.cleanHandCard();
        }
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.Gang, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        mSetPos.getSet().getLastOpInfo().getLastOpGangItem().setLastOpGangItem(mSetPos.getPosID(), cardID, prePublicCard.get(1), OpType.Gang);
        mSetPos.setOpCardId(0);
        return true;
    }


}
