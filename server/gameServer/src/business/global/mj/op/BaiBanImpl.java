package business.global.mj.op;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.manage.BaiBan;
import cenum.mj.MJSpecialEnum;

import java.util.ArrayList;
import java.util.List;

public class BaiBanImpl implements BaiBan {

    @Override
    public List<MJCard> baiBan(AbsMJSetPos mSetPos, int Jin, int Jin2) {
        List<MJCard> privateCards = mSetPos.getPrivateCard();
        if (Jin <= 0) {
            return privateCards;
        }
        if (Jin == MJSpecialEnum.BAIBAN.value()) {
            return privateCards;
        }

        List<MJCard> baiBans = new ArrayList<MJCard>();
        for (MJCard mjCard : privateCards) {
            if (mjCard.type == MJSpecialEnum.BAIBAN.value()) {
                baiBans.add(mjCard);
            }
        }
        if (baiBans.size() <= 0) {
            return privateCards;
        }
        privateCards.removeAll(baiBans);

        boolean isBaiBan = true;
        int max = 0;
        for (int i = 0, size = privateCards.size(); i < size; i++) {
            // 找比金大的牌，然后插入到他前面。
            if (privateCards.get(i).type > Jin
                    && privateCards.get(i).type < MJSpecialEnum.NOT_HUA.value()) {
                isBaiBan = false;
                privateCards.addAll(i, baiBans);
                break;
            }
            if (privateCards.get(i).type > max) {
                max = privateCards.get(i).type;
            }
        }

        if (isBaiBan) {
            privateCards.addAll(baiBans);
        }
        return privateCards;
    }

}
