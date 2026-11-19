package business.global.mj.set;

import cenum.mj.OpType;
import lombok.Data;

/**
 * 最后操作杠的项
 */
@Data
public class LastOpGangItem {

    /**
     * 操作位置Id
     */
    private int opPosId;
    /**
     * 操作牌Id
     */
    private int opCardId;

    /**
     * 被杠的位置Id
     */
    private int fromPos;

    /**
     * 杠类型
     */
    private OpType opType = OpType.Not;

    public LastOpGangItem() {
    }

    public void setLastOpGangItem(int opPosId, int opCardId, int fromPos, OpType opType) {
        this.opPosId = opPosId;
        this.opCardId = opCardId;
        this.fromPos = fromPos;
        this.opType = opType;
    }

    public void clear() {
        this.opPosId = -1;
        this.opType = OpType.Not;
    }


}
