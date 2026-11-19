package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;

/***
 * 推广员房间分成统计表
 * 推送给客户端的 字段减少
 */
@Data
public class ClubRoomPromotionPointInfo {
    private long pid = 0;//玩家pid
    private String dateTime;//日期(字符串yyyyMMdd)
    private double num = 0; // 变化数量值
    private double curRemainder = 0; // 当前剩余
    private double preValue = 0; // 之前的值
    private long roomId;//房间id
    private String roomName = "";//房间名字
    private String roomKey = "";//房间号
    private int execTime;//这条记录产生的时间(秒)
    private long maxID;
    private long minID;
    private int execType = UnionDefine.UNION_EXEC_TYPE.UNION_ENTREE_FEE_NEW.value();
    private int getType = 0;/// 操作日期 0 今天 1昨天 2前天

    /***
     *统计表需要显示的数据
     *execTime,roomKey,roomName,num,
     * @return
     */
    public static String getItemsNameForList() {
        return "pid,timestamp AS execTime,roomKey,roomName,num,roomId,curRemainder,preValue,msg,sum(num) as num,max(id) AS maxID,MIN(id) AS minID";
    }

    /***
     *统计表需要显示的数据
     *execTime,roomKey,roomName,num,
     * @return
     */
    public static String getItemsNameForListByType(int getType) {
        return "pid,timestamp AS execTime,roomKey,roomName,num,roomId,curRemainder,preValue,msg,sum(num) as num,max(id) AS maxID,MIN(id) AS minID," + getType + " AS getType";
    }

    /***
     *统计表需要显示的数据
     *execTime,roomKey,roomName,num,
     * @return
     */
    public static String getItemsNameForCountList(int getType) {
        return "pid,execTime AS execTime,roomKey,roomName,num,roomId,curRemainder,preValue,msg," + getType + " AS getType";
    }
}
