package jsproto.c2s.cclass.club;

import lombok.Data;

/***
 * 推广员房间分成统计表
 */
@Data
public class ClubSportsPointChangeZhongZhiLogItem {
    private long pid = 0;
    private long clubId;
    private String dateTime;
    private long unionId;
    private int reason = 0; // 产生原因类型
    private double num = 0; // 数量
    private double curRemainder = 0; // 当前剩余
    private double preValue = 0; // 前值
    private int type = 0; // 类型(1:获得,2:消耗)
    private int gameId;
    private int classificationRegionId;
    private long roomId;
    private long maxID;
    private long minID;
    private int execTime;//这条记录产生的时间

    public static String getItemsName() {
        return "pid,clubId,dateTime,unionId,reason,sum(num) as num,curRemainder,preValue,type,gameId,classificationRegionId,roomId,max(id) AS maxID,MIN(id) AS minID";
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
