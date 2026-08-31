package business.global.mj.robot;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.player.Robot.*;
import business.player.Robot.TileRank.HuaRank;
import business.player.Robot.TileRank.NumberRank;
import business.player.Robot.TileRank.ZiRank;
import cenum.mj.MJSpecialEnum;
import cenum.mj.OpType;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 机器人位置操作
 *
 * @author Administrator
 */
public class MJSetPosRobot {
    protected AbsMJSetPos mSetPos;
    protected NormalWinType winType = NormalWinType.get();
    protected PlayerTiles selfInfo = new PlayerTiles();
    protected Collection<Tile> candidates = Tile.all();


    public MJSetPosRobot(AbsMJSetPos mSetPos) {
        this.mSetPos = mSetPos;
    }


    /**
     * 获取明杠的牌
     *
     * @return
     */
    public int getGangCid() {
        if (this.mSetPos.sizePublicCardList() <= 0) {
            return 0;
        }
        List<Integer> pengList = this.mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Peng.value()).map(k -> k.get(2) / 100).collect(Collectors.toList());
        if (null == pengList || pengList.size() <= 0) {
            return 0;
        }
        MJCard mCard = this.mSetPos.allCards().stream().filter(k -> pengList.contains(k.getType())).findAny().orElse(null);
        return null == mCard ? 0 : mCard.getCardID();
    }

    /**
     * 检查暗杠
     *
     * @return
     */
    public int getAnGangCid() {
        int cardId = 0;
        int cardType = 0;
        //检查所有重复类型的牌，并且分组
        Map<Integer, Long> anGangMap = mSetPos.allCards().stream().collect(
                Collectors.groupingBy(p -> p.getType(), Collectors.counting()));
        if (null == anGangMap) {
            return cardType;
        }
        //遍历出所有所有出现重复类型的牌，并且找出重复数量为 4 的。
        Iterator<Entry<Integer, Long>> entries = anGangMap.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<Integer, Long> entry = entries.next();
            //找到重复数量为4的牌，获取牌型
            if (entry.getValue() == 4L) {
                cardType = entry.getKey();
                break;
            }
        }
        //如果牌型为 == 0
        if (cardType == 0) {
            return cardType;
        }
        //通过牌型来遍历所有的牌，找到相同的牌型，返回牌值。
        for (MJCard mjCard : mSetPos.allCards()) {
            if (mjCard.type == cardType) {
                cardId = mjCard.cardID;
                break;
            }
        }
        return cardId;
    }


    /**
     * 检查吃
     *
     * @return
     */
    public int getChiCid() {
        int cardId = 0;
        int idx = 0;
        //检查吃列表中是否有值
        int size = mSetPos.getPosOpNotice().getChiList().size();
        if (size <= 0) {
            return cardId;
        }
        //随机选一组。
        int rint = java.util.concurrent.ThreadLocalRandom.current().nextInt(size);
        //检查下标值，是否正确
        if (rint >= size || rint <= -1) {
            return cardId;
        }
        List<Integer> chiList = mSetPos.getPosOpNotice().getChiList().get(rint);
        //检查组中是否有值
        if (chiList.size() <= 0) {
            return cardId;
        }
        //返回第一张牌
        cardId = chiList.get(idx);
        return cardId;
    }


    public int getAutoCard() {
        this.selfInfo.getAliveTiles().clear();
        List<MJCard> allCards = mSetPos.allCards();
        for (MJCard mCard : allCards) {
            if (!mSetPos.getSet().getmJinCardInfo().checkJinExist(mCard.getType())) {
                addcard(mCard);
            }
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
        // 白板处理
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(tmp / 100)) {
            int index = tmp % 10;
            tmp = 4700 + index;
        }
        return tmp;
    }

    public int getAutoCard2() {
        List<MJCard> allCards = mSetPos.allCards();
        Collections.reverse(allCards);
        for (MJCard mCard : allCards) {
            if (mSetPos.getPosOpNotice().getBuNengChuList().contains(mCard.getType())) {
                // 跳过不能出的牌列表,不能吃打清一色
                continue;
            }
            return mCard.cardID;
        }
        return 0;
    }

    /**
     * 获取最优先牌，并去除不能打的牌
     *
     * @param cards
     * @return
     */
    public int getAutoCard(List<Integer> cards) {
        this.selfInfo.getAliveTiles().clear();
        List<MJCard> allCards = mSetPos.allCards();
        Iterator it = allCards.iterator();
        while (it.hasNext()) {
            MJCard mCard = (MJCard) it.next();
            if (!mSetPos.getSet().getmJinCardInfo().checkJinExist(mCard.type)) {
                boolean exist = cards.stream().filter(card -> card == mCard.cardID).findFirst().isPresent();
                if (exist) {
                    it.remove();
                } else {
                    addcard(mCard);
                }
            }
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
        // 白板处理
        if (mSetPos.getSet().getmJinCardInfo().checkJinExist(tmp / 100)) {
            int index = tmp % 10;
            tmp = 4700 + index;
        }
        return tmp;
    }


    protected void addcard(MJCard mj) {
        int type = mj.cardID / 1000;
        int no = mj.type % 10;
        int index = mj.cardID % 10;
        // 白板处理
        if (MJSpecialEnum.BAIBAN.value() == mj.type && this.mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            MJCard mCard = this.mSetPos.getSet().getmJinCardInfo().getJinValues().get(0);
            type = mCard.getCardID() / 1000;
            no = mCard.getType() % 10;
        }

        if (type <= 3) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            NumberRank.ofNumber(no)), index - 1));
        } else if (type > 3 && type < 5) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            ZiRank.ofNumber(no)), index - 1));
        } else if (type >= 5) {
            selfInfo.getAliveTiles().add(
                    Tile.of(TileType.of(TileSuit.ofNumber2(type),
                            HuaRank.ofNumber(no)), index - 1));
        }
    }


}
