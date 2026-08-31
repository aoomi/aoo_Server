package core.dispatch.event.localcache;

import business.global.shareroom.LocalRoomMgr;
import business.global.shareroom.ShareRoom;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

/**
 * 本地缓存房间修改
 */
@Data
public class LocalRoomEvent implements BaseExecutor {
    /**
     * 房间数据
     */
    private String roomKey;
    private ShareRoom shareRoom;
    private Boolean isAdd;

    public LocalRoomEvent(String roomKey, ShareRoom shareRoom, Boolean isAdd) {
        this.roomKey = roomKey;
        this.shareRoom = shareRoom;
        this.isAdd = isAdd;
    }

    @Override
    public void invoke() {
        if (isAdd) {
            LocalRoomMgr.getInstance().addShareRoom(shareRoom);
        } else {
            LocalRoomMgr.getInstance().removeShareRoom(roomKey);
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.LOCAL_ROOM.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.LOCAL_ROOM.bufferSize();
    }
}
