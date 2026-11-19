package jsproto.c2s.cclass;

import lombok.Data;

/**
 * 玩家房间战绩
 *
 * @author Huaxing
 */
@Data
public class PlayerRoomRecord {
    private long roomId;    //房间ID
    private int createTime;
    private int endTime;    //结束时间
    private int setCount;    //局数
    private int gameType;    //游戏类型
    private int playerNum;    //人数
    private String dataJsonRes;//总结果
    private String roomKey;    //房间号
    private int type;        //大类型 1、麻将，2、扑克
    private String playerList;//玩家列表
    private long clubId;//俱乐部ID
    private double roomSportsConsume;
    private long unionId;
    private String configName = "";


    public static String getItemsName() {
        return "id as roomId,createTime,endTime,setCount,gameType,playerNum,dataJsonRes,roomKey,type,playerList,clubID as clubId,roomSportsConsume,unionId,configName";
    }


}
