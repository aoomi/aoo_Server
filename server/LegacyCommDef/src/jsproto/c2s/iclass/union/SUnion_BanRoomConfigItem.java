package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛事禁止房间配置数据表
 */
@Data
@NoArgsConstructor
public class SUnion_BanRoomConfigItem extends BaseSendMsg {
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 游戏Id
     */
    private int gameId;
    /**
     * 0:不禁止,1:禁止
     */
    private int isBan = 1;
    /**
     * 房间名称
     */
    private String roomName = "";
    /**
     * 玩法
     */
    private String dataJsonCfg = "";

    public SUnion_BanRoomConfigItem(int isBan) {
        this.isBan = isBan;
        this.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
    }

    public static String getItemsName() {
        return "configId";
    }

}
