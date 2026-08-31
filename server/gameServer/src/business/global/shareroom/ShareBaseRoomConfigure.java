package business.global.shareroom;

import cenum.PrizeType;
import jsproto.c2s.cclass.room.*;
import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/13 11:29
 * @description 共享公告房间配置对象
 */
@Data
public class ShareBaseRoomConfigure<T> implements Serializable {
    // 消耗类型 -房卡 ,金币
    private PrizeType prizeType;
    // 小类型
    private ShareRoomGameType gameType;
    // 房间创建时初始配置
    private BaseCreateRoom baseCreateRoom = null;
    // 共享方式创建
    private String shareBaseCreateRoom = null;
    // 房间类型
    private T baseCreateRoomT = null;

    // 亲友圈房间配置
    private ClubRoomConfig clubRoomCfg = null;
    // 赛事房间配置
    private UnionRoomConfig unionRoomCfg = null;
    // 机器人房间配置
    private RobotRoomConfig robotRoomCfg = null;
    // 竞技场房间配置
    private ArenaRoomConfig<?> arenaRoomCfg = null;
    //泛型类型
    private String baseCreateRoomClassType;
    /**
     * 标记Id
     */
    private int tagId;
}
