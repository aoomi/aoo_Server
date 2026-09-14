package business.global.mj.scjymj;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.global.room.mj.MJRoomPos;
import jsproto.c2s.cclass.room.RoomPosInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 红中麻将 房间内每个位置信息
 *
 * @author Clark
 */

public class SCJYMJRoomPosMgr extends AbsRoomPosMgr {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SCJYMJRoomPosMgr(AbsBaseRoom room) {
        super(room);
    }

    @Override
    protected void initPosList() {
        for (int i = 0; i < this.getPlayerNum(); i++) {
            posList.add(new SCJYMJRoomPos(i, room));
        }
    }

    @Override
    public AbsRoomPos addWatch() {
        SCJYMJRoomPos newWatch = new SCJYMJRoomPos(-1, this.getRoom());
        this.watchList.add(newWatch);
        return newWatch;
    }

    @Override
    protected AbsRoomPos addAll() {
        MJRoomPos allWatch = new MJRoomPos(-1, this.getRoom());
        this.allList.add(allWatch);
        return allWatch;
    }

    @Override
    public List<RoomPosInfo> getNotify_PosList() {
        List<RoomPosInfo> ret = new ArrayList<>();
        for (int i = 0; i < getPlayerNum(); i++) {
            SCJYMJRoomPos pos = (SCJYMJRoomPos) posList.get(i);
            if (null == pos) {
                continue;
            }
            RoomPosInfo tmPos = pos.getNotify_PosInfo();
            ret.add(tmPos);
        }

        return ret;
    }
}
