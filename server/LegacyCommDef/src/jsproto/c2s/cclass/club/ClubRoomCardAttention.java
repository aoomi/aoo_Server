package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 俱乐部信息
 *
 * @author
 */
@Data
public class ClubRoomCardAttention {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 随机的俱乐部标识ID
     */
    private int clubsign;
    /**
     * 俱乐部名称
     */
    private String clubName;
    /**
     * 俱乐部房卡数量
     */
    private int roomcard;
    /**
     * 俱乐部房卡提醒限度
     */
    private int roomcardattention;
}
