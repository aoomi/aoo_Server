package jsproto.c2s.cclass.room;

import java.io.Serializable;
import java.util.List;

/**
 * 房卡房继续按钮功能存储信息实体 上一个房间的相关信息
 *
 * @param
 */
public class ContinueRoomInfo implements Serializable {
    private Long roomID;//上一个房间id
    private BaseRoomConfigure baseRoomConfigure;//继续房间的 上一个房间的配置
    private List<Long> playerIDList;//上一个房间人员的id信息
    private boolean useFlag = false;//这条继续的信息是否被使用 在第一个人使用的时候设为true
    private int roomEndTime;//上一个房间的结束时间  继续保护用到
    private String roomKey;//继续后创建的新的房间的ID

    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    public List<Long> getPlayerIDList() {
        return playerIDList;
    }

    public void setPlayerIDList(List<Long> playerIDList) {
        this.playerIDList = playerIDList;
    }

    public boolean isUseFlag() {
        return useFlag;
    }

    public void setUseFlag(boolean useFlag) {
        this.useFlag = useFlag;
    }

    public int getRoomEndTime() {
        return roomEndTime;
    }

    public void setRoomEndTime(int roomEndTime) {
        this.roomEndTime = roomEndTime;
    }

    public Long getRoomID() {
        return roomID;
    }

    public void setRoomID(Long roomID) {
        this.roomID = roomID;
    }

    public BaseRoomConfigure getBaseRoomConfigure() {
        return baseRoomConfigure;
    }

    public void setBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        this.baseRoomConfigure = baseRoomConfigure;
    }
}
