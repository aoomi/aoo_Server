package jsproto.c2s.cclass.club;

import lombok.Data;

/***
 * 推广员房间分成统计表
 */
@Data
public class ClubRoomPromotionPointCountLogItem {
    private long pid = 0;
    private String dateTime;
    private long clubId;
    private long unionId;
    private double num = 0; // 变化数量值
    private double curRemainder = 0; // 当前剩余
    private double preValue = 0; // 之前的值
    private int type = 0; // 类型(1:获得,2:消耗)
    private int gameId;
    private int classificationRegionId;
    private long roomId;
    private long execPid = 0;
    private String roomName = "";//房间名字
    private String roomKey = "";//房间号
    private String msg = "";
    private long maxID;
    private long minID;
    private int execTime;//这条记录产生的时间

    public static String getItemsName() {
        return "pid,dateTime,preValue,clubId,unionId,gameId,classificationRegionId,roomId,execPid,roomName,roomKey,sum(num) as num,msg,max(id) AS maxID,MIN(id) AS minID";
    }

    /***
     *统计表需要显示的数据
     *execTime,roomKey,roomName,num,
     * @return
     */
    public static String getItemsNameForList() {
        return "pid,execTime,roomKey,roomName,num,roomId,curRemainder,preValue";
    }

}
