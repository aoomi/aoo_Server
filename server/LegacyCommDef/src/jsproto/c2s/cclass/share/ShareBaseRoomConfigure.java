package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:53
 * @description 共享公共房间配置
 */
@Data
public class ShareBaseRoomConfigure {
    // 消耗类型 -房卡 ,金币
    private SharePrizeType prizeType;
    // 小类型
    private ShareGameType gameType;
    // 房间创建时初始配置
    private Object baseCreateRoom;
    // 亲友圈房间配置
    private ShareClubRoomConfig clubRoomCfg;
    // 赛事房间配置
    private ShareUnionRoomConfig unionRoomCfg;
    // 机器人房间配置
    private ShareRobotRoomConfig robotRoomCfg;
    // 竞技场房间配置
    private ShareArenaRoomConfig arenaRoomCfg;

}
