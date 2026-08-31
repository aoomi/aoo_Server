package business.global.room.compat;

import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.global.room.pk.PockerRoom;
import cenum.room.RoomState;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

/**
 * Migration bridge for poker games implemented against the pre-AbsBaseRoom
 * delegate API. This class deliberately owns no room state: every operation
 * delegates to the current authoritative room kernel.
 */
public abstract class LegacyPokerRoomAdapter extends PockerRoom {

    @SuppressWarnings("rawtypes")
    protected LegacyPokerRoomAdapter(BaseRoomConfigure configuration, String roomKey, long ownerId) {
        super(configuration, roomKey, ownerId);
    }

    /** Legacy name retained while a game is being ported. */
    public final AbsRoomPosMgr getPosMgr() {
        return getRoomPosMgr();
    }

    /** Legacy name retained while a game is being ported. */
    @SuppressWarnings("rawtypes")
    public final BaseRoomConfigure getRoomConfigure() {
        return getBaseRoomConfigure();
    }

    /** Legacy name retained while a game is being ported. */
    public final RoomState getState() {
        return getRoomState();
    }

    /** Route broadcasts through the current room position manager. */
    public final void notify2All(BaseSendMsg message) {
        getRoomPosMgr().notify2All(message);
    }

    /** Route private messages through the current room position manager. */
    public final void notify2Pos(int posId, BaseSendMsg message) {
        getRoomPosMgr().notify2Pos(posId, message);
    }

    /** Current equivalent of the legacy positional lookup. */
    public final AbsRoomPos getPos(int posId) {
        return getRoomPosMgr().getPosByPosID(posId);
    }
}
