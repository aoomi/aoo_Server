package jsproto.c2s.cclass.share;

import jsproto.c2s.cclass.club.Club_define;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 11:07
 * @description 共享亲友圈创建房间设置
 */
@Data
public class ShareClubCreateGameSet {
    /**
     * 游戏配置
     */
    private ShareBaseRoomConfigure bRoomConfigure;
    /**
     * 当前设置状态
     */
    private int status = Club_define.Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL.value();
    /**
     * 创建的房间数
     */
    private int roomCount = 0;
    /**
     * 房间创建时间
     */
    private int createTime = 0;
}
