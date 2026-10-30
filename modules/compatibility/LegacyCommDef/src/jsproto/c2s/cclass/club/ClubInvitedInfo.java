package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 俱乐部邀请信息
 */
@Data
public class ClubInvitedInfo {
    /**
     * 随机的俱乐部标识ID
     */
    private long clubId;
    /**
     * 随机的俱乐部标识ID-6为标识
     */
    private int clubsign;
    /**
     * 俱乐部名称
     */
    private String clubName;


    public ClubInvitedInfo() {
        super();
    }

    public ClubInvitedInfo(long clubId, int clubsign, String clubName) {
        super();
        this.clubId = clubId;
        this.clubsign = clubsign;
        this.clubName = clubName;
    }


}
