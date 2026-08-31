package jsproto.c2s.iclass.club;

import lombok.Data;



/**
 * 赛事禁止玩家指定游戏配置
 */
@Data
public class CClub_BanRoomConfigOp  {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 配置Id
     */
    private long configId ;
    /**
     * 0 关闭 1开启
     */
    private int type;



}
