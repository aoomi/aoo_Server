package jsproto.c2s.cclass.pos;

import lombok.Data;

/**
 * 房间 玩家 位置
 *
 * @author Huaxing
 */
@Data
public class RoomPlayerPos {
    private long pid;
    private int pos;
    private String name;
    private String iconUrl;
    private int sex = 0;
    private int point = 0;
    private Double sportsPoint;
    private String clubName;


}
