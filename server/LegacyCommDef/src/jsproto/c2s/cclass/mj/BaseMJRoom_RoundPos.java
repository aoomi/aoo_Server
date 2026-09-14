package jsproto.c2s.cclass.mj;

import cenum.mj.OpType;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
@Data
public class BaseMJRoom_RoundPos {
    // 本次等待
    protected int waitOpPos = -1; // 当前等待操作的人 暗操作，填-1
    protected List<OpType> opList = null;// 可执行者独享，可操作列表
    protected List<List<Integer>> chiList = null;
    protected List<Integer> tingYouList = null;
    protected List<Integer> tingList = null;
    protected int LastOpCard = 0;
    protected OpType opType = OpType.Pass;
    protected int opCard = 0;
    protected HashMap<Integer, List<Integer>> tingCardMap = null;
    protected List<Integer> buChuList = null;
    protected int LastQGHCard = 0;
    private Integer secTotal;

    public List<Integer> getBuChuList() {
        return buChuList;
    }

    public void setBuChuList(List<Integer> buChuList) {
        this.buChuList = buChuList;
    }

    public int getWaitOpPos() {
        return waitOpPos;
    }

    public void setWaitOpPos(int waitOpPos) {
        this.waitOpPos = waitOpPos;
    }

    public List<OpType> getOpList() {
        return opList;
    }

    public void setOpList(List<OpType> opList) {
        if (null == opList || opList.size() <= 0) {
            return;
        }
        this.opList = opList;
    }

    public List<List<Integer>> getChiList() {
        return chiList;
    }

    public void setChiList(List<List<Integer>> chiList) {
        if (null == chiList || chiList.size() <= 0) {
            return;
        }
        this.chiList = chiList;
    }

    public List<Integer> getTingYouList() {
        return tingYouList;
    }

    public void setTingYouList(List<Integer> tingYouList) {
        if (null == tingYouList || tingYouList.size() <= 0) {
            return;
        }
        this.tingYouList = tingYouList;
    }

    public int getLastOpCard() {
        return LastOpCard;
    }

    public void setLastOpCard(int lastOpCard) {
        LastOpCard = lastOpCard;
    }

    public OpType getOpType() {
        return opType;
    }

    public void setOpType(OpType opType) {
        this.opType = opType;
    }

    public int getOpCard() {
        return opCard;
    }

    public void setOpCard(int opCard) {
        this.opCard = opCard;
    }

    public List<Integer> getTingList() {
        return tingList;
    }

    public void setTingList(List<Integer> tingList) {
        if (null == tingList || tingList.size() <= 0) {
            return;
        }
        this.tingList = tingList;
    }

    public HashMap<Integer, List<Integer>> getTingCardMap() {
        return tingCardMap;
    }

    public void setTingCardMap(HashMap<Integer, List<Integer>> tingCardMap) {
        if (null == tingCardMap || tingCardMap.size() <= 0) {
            return;
        }
        this.tingCardMap = tingCardMap;
    }

    public void setLastQGHCard(int lastQGHCard) {
        LastQGHCard = lastQGHCard;
    }


    @Override
    public String toString() {
        return "BaseMJRoom_RoundPos [waitOpPos=" + waitOpPos + ", opList=" + opList + ", chiList=" + chiList
                + ", tingYouList=" + tingYouList + ", LastOpCard=" + LastOpCard + ", opType=" + opType + ", opCard="
                + opCard + "]";
    }

}
