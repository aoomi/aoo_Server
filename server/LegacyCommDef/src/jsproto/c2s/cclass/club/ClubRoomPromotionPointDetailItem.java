package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;

/***
 * 推广员房间分成统计表
 */
@Data
public class ClubRoomPromotionPointDetailItem {
    private long pid = 0;//玩家pid
    private double num = 0; // 变化数量值
    private long execPid = 0;//参与游戏的玩家(预留值)
    private int type; //0 默认状态 1 上级分成 2 下级分成 3 房费
    private long reasonPid = 0;//type为1 2时候对应的玩家pid
    private String reasonPidName = "";//type为1 2时候对应的玩家名字
    private int execType = UnionDefine.UNION_EXEC_TYPE.UNION_ENTREE_FEE_NEW_DETAIL.value();

    public static String getItemsName() {
        return "pid,num,execPid,reasonPid";
    }


}
