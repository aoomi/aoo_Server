package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_RoomInfoDetails extends BaseSendMsg {
    /**
     * 房间可以
     */
    private String roomKey;

    private long clubId;
}
