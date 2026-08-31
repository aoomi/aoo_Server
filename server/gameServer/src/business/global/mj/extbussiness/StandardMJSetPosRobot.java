package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.robot.MJSetPosRobot;
import business.player.Robot.Tile;
import business.player.Robot.TileRank;
import business.player.Robot.TileSuit;
import business.player.Robot.TileType;

import java.util.ArrayList;
import java.util.List;

/**
 * 麻将机器人位置操作
 *
 * @author Administrator
 */
public class StandardMJSetPosRobot extends MJSetPosRobot {
    public StandardMJSetPosRobot(AbsMJSetPos mSetPos) {
        super(mSetPos);
    }
    @Override
    public int getAutoCard() {
        List<Integer> buNengChuList = new ArrayList<>(((StandardMJSetPos)mSetPos).getBuNengChuList());
        if(mSetPos.getHandCard()!=null&&!buNengChuList.contains(mSetPos.getHandCard().getType())){
            return mSetPos.getHandCard().cardID;
        }
        for (MJCard mCard : mSetPos.allCards()) {
            if (!buNengChuList.contains(mCard.type)) {
                return mCard.cardID;
            }
        }
        return mSetPos.getHandCard()!=null?mSetPos.getHandCard().cardID:0;
    }

    @Override
    public int getAutoCard2() {
        List<Integer> buNengChuList = new ArrayList<>(((StandardMJSetPos) mSetPos).getBuNengChuList());
        if (mSetPos.getHandCard() != null && !buNengChuList.contains(mSetPos.getHandCard().getType())) {
            return mSetPos.getHandCard().cardID;
        }
        List<MJCard> allCards = mSetPos.allCards();
        for (MJCard mCard : allCards) {
            if (!buNengChuList.contains(mCard.type)) {
                return mCard.cardID;
            }
        }
        return mSetPos.allCards().get(0).getCardID();
    }

    protected void addcard(MJCard mj) {
        int type = mj.cardID / 1000;
        int no = mj.type % 10;
        int index = mj.cardID % 10;

        if (type <= 3) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            TileRank.NumberRank.ofNumber(no)), index - 1));
        } else if (type > 3 && type < 5) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            TileRank.ZiRank.ofNumber(no)), index - 1));
        } else if (type >= 5) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            TileRank.HuaRank.ofNumber(no)), index - 1));
        }
    }
}
