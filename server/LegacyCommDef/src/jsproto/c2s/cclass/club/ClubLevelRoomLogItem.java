package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubLevelRoomLogItem {
    private long id;
    /**
     * 服务Id
     */
    private int server_id;
    /**
     * 时间戳
     */
    private int timestamp;
    /**
     * 时间
     */
    private String date_time;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 大赢家数
     */
    private int winner;
    /**
     * 消费
     */
    private int consume;
    private long roomId;
    private long upLevelId;
    private long memberId;
    private int point;
    private double sportsPoint = 0D;
    private int setCount;
    private double sportsPointConsume = 0D;
    private double roomSportsPointConsume = 0D;
    private double roomAvgSportsPointConsume = 0D;
    private long clubId;
    private long transferId;

    public static String getItemsName() {
        return "id,server_id,timestamp,date_time,pid,winner,consume,roomId,upLevelId,memberId,point,sportsPoint,setCount,sportsPointConsume,roomSportsPointConsume,roomAvgSportsPointConsume,clubId";
    }
}
