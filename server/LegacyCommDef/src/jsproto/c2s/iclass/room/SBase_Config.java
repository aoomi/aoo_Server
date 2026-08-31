package jsproto.c2s.iclass.room;

import cenum.RoomTypeEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import lombok.Data;

@SuppressWarnings("serial")
@Data
public class SBase_Config extends BaseSendMsg {
    private BaseCreateRoom cfg;
    private boolean isClub;
    private int roomType;


    public void setRoomType(RoomTypeEnum roomTypeEnum) {
        if (RoomTypeEnum.CLUB.equals(roomTypeEnum)) {
            this.isClub = true;
        }
        this.roomType = roomTypeEnum.ordinal();
    }
}