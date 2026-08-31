package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

/**
 * 俱乐部信息
 *
 * @author
 */
@Data
public class ClubRecordByPlayerRank {
    /**
     * 俱乐部房卡数量
     */
    private int roomCard;
    /**
     * 圈卡
     */
    private int clubCard;
    /**
     * 俱乐部大赢家次数
     */
    private int winCount;
    /**
     * 玩家信息
     */
    private ShortPlayer shortPlayer;


    public ClubRecordByPlayerRank(int roomCard, int clubCard, int winCount, ShortPlayer shortPlayer) {
        super();
        this.roomCard = roomCard;
        this.clubCard = clubCard;
        this.winCount = winCount;
        this.shortPlayer = shortPlayer;
    }
}
