package jsproto.c2s.cclass.luckdraw;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖品信息
 */
@Data
@NoArgsConstructor
public class LuckDrawInfo {
    /**
     * 商品Id
     */
    private long id;
    /**
     * 奖品名称
     */
    private String prizeName;
    /**
     * 奖品类型
     */
    private int prizeType;
    /**
     * 奖品数量
     */
    private int rewardNum;

    public LuckDrawInfo(long id, String prizeName, int prizeType, int rewardNum) {
        this.id = id;
        this.prizeName = prizeName;
        this.prizeType = prizeType;
        this.rewardNum = rewardNum;
    }
}
