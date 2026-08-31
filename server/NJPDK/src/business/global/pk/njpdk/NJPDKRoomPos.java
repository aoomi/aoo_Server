package business.global.pk.njpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.njpdk.c2s.cclass.NJPDKRoom_PosEnd;
import com.aoo.bcg.common.reconnect.CardPerspective;
import jsproto.c2s.cclass.pk.BasePockerLogic;

import java.util.ArrayList;
import java.util.List;

/**
 * 资阳大局位置
 */
public class NJPDKRoomPos extends AbsRoomPos {

    private ArrayList<Integer> privateCards = new ArrayList<>(); // 权威手牌
    private ArrayList<Integer> backupsCards = new ArrayList<>(); // 发牌快照
    private int m_nWin = 0; // 赢场数
    private int m_nLose = 0; // 输场数
    private int m_nFlat = 0; // 平场数
    public boolean isBaoPei = false;
    public int bombPoint = 0; //炸弹输赢分
    public int maxPoint = 0; //单局最高
    public int type = 0; //0:没有 1:关牌 2：春天 3: 反春
    public boolean isBaoDan = false; // 报单，包双

    public boolean canGuan = false;

    public NJPDKRoomPos(int posID, AbsBaseRoom room) {
        super(posID, room);
    }

    /**
     * 初始化手牌init
     *
     * @param cards
     */
    public void init(List<Integer> cards) {
        this.privateCards = new ArrayList<>(cards);
        this.backupsCards = new ArrayList<>(cards);
    }

    /**
     * 初始化手牌
     */
    public void addCard(Integer card) {
        this.privateCards.add(card);
        this.backupsCards.add(card);
    }

    /**
     * 获取牌组信息
     *
     * @param pid 玩家id
     * @return
     */
    public ArrayList<Integer> getNotifyCard(long pid) {
        return CardPerspective.hand(privateCards, this.getPid() == pid);
    }

    /**
     * 删除牌组信息,这里删除了，不管失败成功，privateCards都会改变
     *
     * @param cardList
     * @return
     */
    public boolean deleteCard(ArrayList<Integer> cardList) {
        long count = cardList.stream().distinct().count();
        if (count == cardList.size() && this.privateCards.containsAll(cardList)) {
            return privateCards.removeAll(cardList);
        }
        return false;
    }

    /**
     * 判断玩家手牌是否拥有该牌,花色和牌值都相等的
     *
     * @param card 指定牌
     * @return
     */
    public boolean checkCard(Integer card) {
        return BasePockerLogic.getCardCount(privateCards, card, false) > 0;
    }

    /**
     * 大局结算
     *
     * @return
     */
    public NJPDKRoom_PosEnd calcPosEnd() {
        NJPDKRoomSet set = (NJPDKRoomSet) this.getRoom().getCurSet();
        this.calcRoomPoint(set.pointList.get(this.getPosID()));

        NJPDKRoom_PosEnd posEnd = new NJPDKRoom_PosEnd();
        int setPoint = set.pointList.get(this.getPosID());
        posEnd.point = setPoint;
        posEnd.pos = this.getPosID();
        posEnd.pid = this.getPid();
        posEnd.surplusCardList = set.surplusCardRecordList;
        posEnd.type = type;
        posEnd.sportsPoint = setSportsPoint(setPoint);

        return posEnd;
    }

    public ArrayList<Integer> getPrivateCards() {
        return new ArrayList<>(privateCards);
    }

    ArrayList<Integer> cards() { return privateCards; }

    ArrayList<Integer> dealtCards() { return backupsCards; }

    public int getWin() {
        return m_nWin;
    }

    public void addWin(int nWin) {
        this.m_nWin += nWin;
    }

    public int getLose() {
        return m_nLose;
    }

    public void addLose(int nLose) {
        this.m_nLose += nLose;
    }

    /**
     *
     */
    public void addFlat(int nFlat) {
        this.m_nFlat += nFlat;
    }

    public int getFlat() {
        return m_nFlat;
    }

    public void setMaxPoint(int maxPoint) {
        this.maxPoint = this.maxPoint > maxPoint ? this.maxPoint : maxPoint;
    }
}
