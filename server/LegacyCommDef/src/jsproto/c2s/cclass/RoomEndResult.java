package jsproto.c2s.cclass;

import java.util.List;

public class RoomEndResult<T> extends BaseSendMsg {
    private long roomId;
    private String key;
    private int setId;
    private int endTime;
    private long ownerID;
    private List<T> resultsList; // 斗地主名字叫countRecords
    private int newSetID;//显示的setID 用于连庄局数不增加 但是后台要记录的情况

    public int getNewSetID() {
        return newSetID;
    }

    public void setNewSetID(int newSetID) {
        this.newSetID = newSetID;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getSetId() {
        return setId;
    }

    public void setSetId(int setId) {
        this.setId = setId;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public List<T> getResultsList() {
        return resultsList;
    }

    public void setResultsList(List<T> resultsList) {
        this.resultsList = resultsList;
    }

    public long getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(long ownerID) {
        this.ownerID = ownerID;
    }

    public void clear() {
        // 清空解算历史
        if (null != this.resultsList) {
            this.resultsList.clear();
            this.resultsList = null;
        }
    }

    @Override
    public String toString() {
        return "RoomEndResult{" +
                "roomId=" + roomId +
                ", key='" + key + '\'' +
                ", setId=" + setId +
                ", endTime=" + endTime +
                ", ownerID=" + ownerID +
                ", resultsList=" + resultsList +
                '}';
    }
}
