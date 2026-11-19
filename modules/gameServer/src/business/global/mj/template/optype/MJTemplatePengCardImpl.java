package business.global.mj.template.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.op.PengCardImpl;
import business.global.mj.set.LastOpTypeItem;
import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateSetPos;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查暗杠
 *
 * @author Administrator
 */
public class MJTemplatePengCardImpl extends PengCardImpl {
    @Override
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        if (setPos.isTing()) {
            return false;
        }
        int type = cardId / 100;
        if (checkFilter(mSetPos, cardId)) {
            return false;
        }
        long count = mSetPos.allCards().stream()
                // 筛选出所有的牌类型
                .map(k -> k.getType())
                // 检查不等于金牌
                .filter(k -> k == type)
                // 按牌类型分组
                .count();
        return count >= 2L;
    }

    @Override
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID) {
        int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
        int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).getOwnnerPos();
        int type = lastOutCard / 100;
        if (!checkFilter(mSetPos, type)) {
            // 金牌或者花牌。
            return false;
        }
        List<Integer> publicCard = new ArrayList<>();
        publicCard.add(OpType.Peng.value());
        publicCard.add(fromPos);
        publicCard.add(cardID);
        publicCard.add(cardID);
        // 搜集牌
        List<Integer> tmp = mSetPos.allCards().stream()
                .map(k -> k.getCardID()).filter(k -> k / 100 == type)
                .limit(2).collect(Collectors.toList());
        // 牌 < 2.
        if (tmp.size() < 2) {
            return false;
        }

        // 有接杠不接杠选择了碰。
        if (mSetPos.getSet().getLastOpInfo().checkBuGang(type, OpType.JieGang)) {
            // 不能杠了。
            mSetPos.getSet().getLastOpInfo().addBuGang(type, OpType.Not);
        }
        // 增加亮牌
        publicCard.addAll(tmp);
        mSetPos.addPublicCard(publicCard);
        mSetPos.removeAllPrivateCards(tmp);
        mSetPos.privateMoveHandCard();
        mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.Peng, new LastOpTypeItem(mSetPos.getPosID(), cardID));
        return true;

    }

    /**
     * 检查过滤器
     */
    protected boolean checkFilter(AbsMJSetPos mSetPos, int type) {
        if (((MJTemplateRoom) mSetPos.getRoom()).isWanFa_JinBuKeChiPengGang() && mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
            return false;
        }
        if (((MJTemplateSetPos) mSetPos).checkIsQue(type)) {
            return false;
        }
        return type < MJSpecialEnum.NOT_HUA.value();
    }
}
