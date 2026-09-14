package jsproto.c2s.cclass.club;

import cenum.PrizeType;
import lombok.Data;

/**
 * 俱乐部信息
 */
@Data
public class ClubRecordInfo {
    /**
     * 房间ID
     */
    private long roomID;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 玩法
     */
    private String cfg;
    /**
     * 玩家列表
     */
    private String playerList;
    /**
     * 结束时间
     */
    private int endTime;
    /**
     * 房卡
     */
    private int roomCard;
    /**
     * 圈卡
     */
    private int clubCard;
    /**
     * 游戏类型
     */
    private int gameType;
    /**
     * 房主
     */
    private long ownner;
    /**
     * 值类型
     */
    private int valueType;
    /**
     * 1:游戏中,2:游戏结束(只处理状态==1)
     */
    private int roomState;
    /**
     * 房间竞技点消耗
     */
    private double roomSportsConsume;

    /**
     * 赛事Id
     */
    private long unionId;

    /**
     * 已查看
     */
    private int isViewed = 0;
    /**
     * 竞技点倍数
     */
    private Double sportsDouble;
    /**
     * 房间房间配置名称
     */
    private String configName = "";

    public ClubRecordInfo(long roomID, String roomKey, String playerList, int endTime, int consumeValue,
                          int valueType, int gameType, long ownner, int roomState, double roomSportsConsume, long unionId, String configName) {
        super();
        this.roomID = roomID;
        this.roomKey = roomKey;
        this.playerList = playerList;
        this.endTime = endTime;
        if (PrizeType.RoomCard.value() == valueType) {
            this.roomCard = consumeValue;
        } else {
            this.clubCard = consumeValue;
        }
        this.valueType = valueType;
        this.gameType = gameType;
        this.ownner = ownner;
        this.roomState = roomState;
        this.roomSportsConsume = roomSportsConsume;
        this.unionId = unionId;
        this.configName = configName;
    }

    public ClubRecordInfo(long roomID, String roomKey, String cfg, String playerList, int endTime, int consumeValue,
                          int valueType, int gameType, long ownner, int roomState, double roomSportsConsume, long unionId) {
        super();
        this.roomID = roomID;
        this.roomKey = roomKey;
        this.cfg = cfg;
        this.playerList = playerList;
        this.endTime = endTime;
        if (PrizeType.RoomCard.value() == valueType) {
            this.roomCard = consumeValue;
        } else {
            this.clubCard = consumeValue;
        }
        this.valueType = valueType;
        this.gameType = gameType;
        this.ownner = ownner;
        this.roomState = roomState;
        this.roomSportsConsume = roomSportsConsume;
        this.unionId = unionId;
    }

    public ClubRecordInfo(long roomID, String roomKey, String cfg, String playerList, int endTime, int consumeValue,
                          int valueType, int gameType, long ownner, int roomState, double roomSportsConsume, long unionId, boolean isViewed, Double sportsDouble, String configName) {
        super();
        this.roomID = roomID;
        this.roomKey = roomKey;
        this.cfg = cfg;
        this.playerList = playerList;
        this.endTime = endTime;
        if (PrizeType.RoomCard.value() == valueType) {
            this.roomCard = consumeValue;
        } else {
            this.clubCard = consumeValue;
        }
        this.valueType = valueType;
        this.gameType = gameType;
        this.ownner = ownner;
        this.roomState = roomState;
        this.roomSportsConsume = roomSportsConsume;
        this.unionId = unionId;
        this.isViewed = isViewed ? 1 : 0;
        this.sportsDouble = sportsDouble;
        this.configName = configName;
    }

    public static String getItemsNameLeftJoin() {
        return "r.id,ownner,r.gameType,r.roomKey,playerList,endTime,consumeValue,valueType,r.roomState,r.roomSportsConsume,r.unionId";
    }


    public static String getItemsName() {
        return "id,ownner,gameType,roomKey,dataJsonCfg,playerList,endTime,consumeValue,valueType,roomState,roomSportsConsume,unionId,configName";
    }

    public static String getNotCfgItemsName() {
        return "id,ownner,gameType,roomKey,playerList,endTime,consumeValue,valueType,roomState,roomSportsConsume,unionId,configName";
    }

}
