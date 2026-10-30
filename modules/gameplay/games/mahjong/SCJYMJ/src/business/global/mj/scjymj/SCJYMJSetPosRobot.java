package business.global.mj.scjymj;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.robot.MJSetPosRobot;
import business.player.Robot.Tile;
import business.player.Robot.TileRank.NumberRank;
import business.player.Robot.TileRank.ZiRank;
import business.player.Robot.TileSuit;
import business.player.Robot.TileType;

import java.util.List;

/**
 * 机器人位置操作
 *
 * @author Administrator
 */
public class SCJYMJSetPosRobot extends MJSetPosRobot {
    public SCJYMJSetPosRobot(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }

    @Override
    public int getAutoCard() {
        this.selfInfo.getAliveTiles().clear();
        if (((SCJYMJSetPos) mSetPos).isTing()) {
            if (null != this.mSetPos.getHandCard()) {
                return this.mSetPos.getHandCard().cardID;
            }
        }
        SCJYMJSetPos aPos = ((SCJYMJSetPos) mSetPos);

        List<MJCard> allCards = mSetPos.allCards();
        for (MJCard mCard : allCards) {
            addcard(mCard);
        }
        int tmp = 0;
        List<Tile> lst = winType.getDiscardCandidates(selfInfo.getAliveTiles(), candidates);
        if (lst.size() <= 0) {
            if (mSetPos.sizePrivateCard() > 0) {
                MJCard mCard = mSetPos.getPCard(mSetPos.sizePrivateCard() - 1);
                if (null != mCard) {
                    tmp = mCard.cardID;
                }
            }
        } else {
            tmp = lst.get(0).cardId();
        }
        if (this.mSetPos.getPosOpNotice().getBuNengChuList().contains(tmp / 100)) {
            for (MJCard mCard : allCards) {
                if (!this.mSetPos.getPosOpNotice().getBuNengChuList().contains(mCard.type)) {
                    return mCard.cardID;
                }
            }
        }


        return tmp;
    }


    @Override
    protected void addcard(MJCard mj) {
        int type = mj.cardID / 1000;
        int no = mj.type % 10;
        int index = mj.cardID % 10;
        if (type <= 3) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            NumberRank.ofNumber(no)), index - 1));
        } else if (type > 3) {
            if (type >= 5 || no > 7) {
                return;
            }

            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            ZiRank.ofNumber(no)), index - 1));
        }
    }


}
