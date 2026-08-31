package business.njpdk.c2s.cclass;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局中每个位置信息
 *
 * @author zaf
 */
public class NJPDKRoomSet_Pos {

    public int posID = 0;                    // 座号ID
    public long pid = 0;                    // 账号
    public List<Integer> cards = new ArrayList<>();    //牌
    public int point;                        //积分
    public boolean guanpai;
    public ArrayList<Integer> surplusCardList = new ArrayList<Integer>();        //剩余牌数
    /**
     * 竞技点
     */
    public Double sportsPoint;

    public NJPDKRoomSet_Pos(int posID, long pid, List<Integer> cards, int point, ArrayList<Integer> surplusCardList) {
        super();
        this.posID = posID;
        this.pid = pid;
        this.cards = cards;
        this.point = point;
        this.surplusCardList = surplusCardList;
    }

    public NJPDKRoomSet_Pos() {
    }

}
