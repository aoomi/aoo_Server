package jsproto.c2s.iclass;

import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomSetInfo;

/**
 * 获取红中麻将房间信息
 *
 * @author Administrator
 */
@SuppressWarnings("rawtypes")
public class S_GetRoomInfo extends GetRoomInfo {
    private RoomSetInfo set;

    public RoomSetInfo getSet() {
        return set;
    }

    public void setSet(RoomSetInfo set) {
        this.set = set;
    }

}