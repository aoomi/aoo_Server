package business.global.mj.robot;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;

import java.util.Collections;
import java.util.List;

/**
 * 机器人位置操作
 *
 * @author Administrator
 */
public class MJTemplateSetPosRobot extends MJSetPosRobot {


    public MJTemplateSetPosRobot(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }

    public int getAutoCard2() {
        List<MJCard> allCards = mSetPos.allCards();
        Collections.reverse(allCards);
        for (MJCard mCard : allCards) {
            //不出列表 为cardId
            if (mSetPos.getPosOpNotice().getBuNengChuList().stream().anyMatch(k -> k != null && (k.intValue() == mCard.getCardID() || k.intValue() == mCard.getType()))) {
                // 跳过不能出的牌列表,不能吃打清一色
                continue;
            }
            return mCard.cardID;
        }
        return 0;
    }


}
