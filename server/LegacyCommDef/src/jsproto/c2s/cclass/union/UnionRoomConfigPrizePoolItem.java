package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UnionRoomConfigPrizePoolItem implements Serializable {
    /**
     * 配置Id
     */
    private long configId = 0;
    /**
     * 房间名称
     */
    private String roomName = "";
    /**
     * 玩法
     */
    private String dataJsonCfg = "";
    /**
     * 对局数
     */
    private int setCount = 0;
    /**
     * 开房数
     */
    private int roomSize = 0;
    /**
     * 消耗数(中至:房卡消耗)
     */
    private int consumeValue = 0;
    /**
     * 奖金池(中至:联赛活跃积分)
     */
    private double prizePool;
    /**
     * 游戏id
     */
    private int gameId;
    /**
     * 标记Id
     */
    private int tagId;
    /**
     * 总洗牌费
     */
    private double xiPaiIncome;

    /**
     * 总收益分成
     */
    private double sportsPointIncome;
    /**
     * 最终积分总和(成员总积分和+活跃度)
     */
    private double finalAllMemberPointTotal;
    /**
     * 成员总积分和(输赢分-房费)
     */
    private double unionAllMemberPointTotal;

    public static String getItemsName() {
        return "gameId,configId,roomName,dataJsonCfg,sum(setCount) as setCount,sum(roomSize) as roomSize,sum(consumeValue) as consumeValue,sum(prizePool) as prizePool";
    }

    public static String getItemsNameCount() {
        return "sum(setCount) as setCount,sum(roomSize) as roomSize,sum(consumeValue) as consumeValue,sum(prizePool) as prizePool";
    }

    public static String getXiPaiCount() {
        return "sum(value) as xiPaiIncome";
    }
}
