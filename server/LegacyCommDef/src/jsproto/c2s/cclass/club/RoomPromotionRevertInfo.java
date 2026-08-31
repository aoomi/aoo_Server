package jsproto.c2s.cclass.club;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RoomPromotionRevertInfo {
    /**
     * 房间比赛分消耗
     */
    private double roomSportsPointConsume;

    /**
     * 房间比赛分消耗
     */
    private double roomSportsPointConsumePrizePool;
    /**
     * 赛事id
     */
    private long unionId;
    /**
     * 每个玩家对应的亲友圈id
     */
//    private List<PidMap> pidMap = new ArrayList<>();//key 值为pid value 为clubid
    @Builder.Default
    private Map<Long, Long> pidMap = new HashMap<>();

    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间号
     */
    private int roomKey;
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 房间id
     */
    private long roomId;
    /**
     * 每个亲友圈对应的玩家id列表
     */
//    private List<ClubMap> clubMap = new ArrayList<>();//key 值为clubid value 为pid
    @Builder.Default
    private Map<Long, List<Long>> clubMap = new HashMap<>();
    /**
     * ID
     */
    private int Id;
    /**
     * 玩法地区分类统计ID，不参与身份、资产或路由
     */
    private int classificationRegionId;
    /**
     * 奖金池
     */
    private double prizePool;
    /**
     * 收益类型
     */
    @Builder.Default
    private int sourceType = 2;
    /**
     * 时间
     */
    private String dateTime;
    /**
     * 对局数
     */
    private int setCount;
    /**
     * 房间配置
     */
    private String dataJsonCfg;
    /**
     * 消耗钻石数
     */
    private int consumeValue;
    /**
     * 游戏类型
     */
    private int gameId;
    /**
     * 盟主所获收益
     */
    private double unionCreateIncome;


}
