package business.global.pk.njpdk;

import business.njpdk.c2s.cclass.NJPDK_define;
import business.njpdk.c2s.iclass.SNJPDK_CardNumber;
import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Maps;
import jsproto.c2s.cclass.pk.BasePocker.PockerListType;
import jsproto.c2s.cclass.pk.BasePockerLogic;

import java.util.*;

/**
 * 资阳跑得快，设置牌
 *
 * @author Huaxing
 */
public class NJPDKSetCard {

    public ArrayList<Integer> leftCards = new ArrayList<Integer>(); // 扑克牌编号
    public ArrayList<Integer> backUpCards = new ArrayList<Integer>(); // 扑克牌编号
    private final GameRandomSource random;
    public NJPDKRoom zRoom;
    private Map<Integer, Integer> cardNumMap = Maps.newConcurrentMap();
    private int normalCardNum = 48;//去掉3A和1张2的牌和赖子剩余牌数
    private int singleNum = 4;//单牌最大张数
    private List<Integer> removeCard = Arrays.asList(0x0E, 0x0F, 0x1F, 0x2F);//去除的牌

    public NJPDKSetCard(NJPDKRoom room) {
        this(room, SeededGameRandomSource.create());
    }

    NJPDKSetCard(NJPDKRoom room, GameRandomSource random) {
        this.random = Objects.requireNonNull(random, "random");
        zRoom = room;
        if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
            removeCard = Arrays.asList(0x0D, 0x0E, 0x1E, 0x2E, 0x0F, 0x1F, 0x2F);//去除的牌
        }
        randomCard();
    }


    /**
     * 生成牌堆
     */
    @SuppressWarnings("unchecked")
    public void randomCard() {
        cardNumMap = Maps.newConcurrentMap();
        this.leftCards = BasePockerLogic.getRandomPockerList(1, 0, PockerListType.POCKERLISTTYPE_TWOEND);
        //去3张1 1张2 ，52变48
        for (int byte1 : removeCard) {
            BasePockerLogic.deleteSameCard(this.leftCards, byte1, false);
        }
        int totalCards = leftCards.size();
        //勾线记牌器功能才设置剩余牌数
        if (zRoom.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.JiPaiQi.getType())) {
            //记牌器
            for (int i = 0; i < this.leftCards.size(); i++) {
                Integer card = this.leftCards.get(i);
                int cardKey = BasePockerLogic.getCardValue(card);
                int cardNum = cardNumMap.get(cardKey) != null ? cardNumMap.get(cardKey) + 1 : 1;
                cardNumMap.put(cardKey, cardNum);
            }
            totalCards = cardNumMap.values().stream().reduce(0, (x, y) -> x + y);
        }
        //如果牌值不等于默认牌张数
        if (totalCards != normalCardNum) {
            CommLogD.error("The number of CARDS exceeds no equal " + normalCardNum);
        }
    }

    public void clean() {
        if (null != this.leftCards) {
            this.leftCards.clear();
            this.leftCards = null;
        }
        if (null != this.backUpCards) {
            this.backUpCards.clear();
            this.backUpCards = null;
        }
        if (null != this.cardNumMap) {
            this.cardNumMap.clear();
            this.cardNumMap = null;
        }
    }


    /*
     * 洗牌
     * **/
    @SuppressWarnings("unchecked")
    public void onXiPai() {
        random.shuffle(this.leftCards);
        this.backUpCards = (ArrayList<Integer>) this.leftCards.clone();
    }

    /**
     * 发牌
     *
     * @param cnt
     * @return
     */
    public ArrayList<Integer> popList(int cnt) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        if (this.leftCards.size() <= 0) return ret;
        for (int i = 0; i < cnt; i++) {
            if (this.leftCards.size() <= 0) return ret;
            Integer byte1 = this.leftCards.remove(random.nextInt(this.leftCards.size()));
            ret.add(byte1);
        }
        return ret;
    }

    public ArrayList<Integer> getLeftCards() {
        return leftCards;
    }

    /**
     * 记牌器减少牌数
     *
     * @param cardLst
     */
    public void subCardCount(ArrayList<Integer> cardLst) {
        //勾线记牌器功能才通知
        if (zRoom.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.JiPaiQi.getType())) {
            for (Integer byte1 : cardLst) {
                int cardValue = BasePockerLogic.getCardValue(byte1);
                int count = this.cardNumMap.get(cardValue);
                this.cardNumMap.put(cardValue, count - 1);
            }
            notity2CardNumMap();
        }
    }

    /**
     * 通知更新记牌器
     */
    public void notity2CardNumMap() {
        this.zRoom.getRoomPosMgr().notify2All(SNJPDK_CardNumber.make(this.zRoom.getRoomID(), cardNumMap));
    }

    public Map<Integer, Integer> getCardNumMap() {
        return cardNumMap;
    }
}
