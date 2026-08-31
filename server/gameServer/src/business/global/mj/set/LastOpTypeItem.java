package business.global.mj.set;

import lombok.Data;

/**
 * 最近操作项
 */
@Data
public class LastOpTypeItem {
    /**
     * 操作位置Id
     */
    private int opPosId;
    /**
     * 操作牌Id
     */
    private int opCardId;

    public LastOpTypeItem(int opPosId, int opCardId) {
        this.opPosId = opPosId;
        this.opCardId = opCardId;
    }

}
