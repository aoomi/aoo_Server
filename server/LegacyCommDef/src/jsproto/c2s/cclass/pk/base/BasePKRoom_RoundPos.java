package jsproto.c2s.cclass.pk.base;

import cenum.PKOpType;
import lombok.Data;

import java.util.List;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
@Data
public class BasePKRoom_RoundPos {
    // 本次等待
    protected int waitOpPos = -1; // 当前等待操作的人 暗操作，填-1
    protected List<PKOpType> opList = null;// 可执行者独享，可操作列表
    protected PKOpType opType = PKOpType.Pass;
    protected int opCard = 0;

    public int getWaitOpPos() {
        return waitOpPos;
    }

    public void setWaitOpPos(int waitOpPos) {
        this.waitOpPos = waitOpPos;
    }

    public List<PKOpType> getOpList() {
        return opList;
    }

    public void setOpList(List<PKOpType> opList) {
        if (null == opList || opList.size() <= 0) {
            return;
        }
        this.opList = opList;
    }

    public PKOpType getOpType() {
        return opType;
    }

    public void setOpType(PKOpType opType) {
        this.opType = opType;
    }

    public int getOpCard() {
        return opCard;
    }

    public void setOpCard(int opCard) {
        this.opCard = opCard;
    }


    @Override
    public String toString() {
        return "BaseMJRoom_RoundPos [waitOpPos=" + waitOpPos + ", opList=" + opList
                + ", PKOpType=" + opType + ", opCard="
                + opCard + "]";
    }

}
