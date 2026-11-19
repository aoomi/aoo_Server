package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_ChangeClubName extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     *俱乐部名字
     */
    private String name="";

    public CClub_ChangeClubName(long clubId, String name) {
        this.clubId = clubId;
        this.name = name;
    }
}
