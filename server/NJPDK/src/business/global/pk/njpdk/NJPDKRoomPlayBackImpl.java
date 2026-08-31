package business.global.pk.njpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.RoomPlayBackImplAbstract;
import jsproto.c2s.cclass.BaseSendMsg;

public class NJPDKRoomPlayBackImpl extends RoomPlayBackImplAbstract {
    private static final String opCard = "OpCard";    //监听的消息
    private final static String SetStart = "SetStart";

    public NJPDKRoomPlayBackImpl(AbsBaseRoom roomPosMgrDelegateAbstract) {
        super(roomPosMgrDelegateAbstract);
    }

    @Override
    public boolean isOpCard(BaseSendMsg msg) {
        if (msg.getOpName().indexOf(opCard) > 0 || msg.getOpName().indexOf(SetStart) > 0) {
            return true;
        }
        return false;
    }
}
