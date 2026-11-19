package jsproto.c2s.cclass;

/**
 * 玩家房间内每局的数据
 *
 * @author Huaxing
 */
public class PlayerSetRoomRecord {

    private long roomID; // 房间ID
    private int setID; // 当前局数
    private int endTime; // 结束时间
    private String dataJsonRes; // 结果数据
    private int playbackCode; // 回放码

    public static String getItemsName() {
        return "roomID,setID,endTime,dataJsonRes,playbackCode as playBackCode";
    }

    public static String getItemsNamePlaybackCode() {
        return "playbackCode as playBackCode";
    }


    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getSetID() {
        return setID;
    }

    public void setSetID(int setID) {
        this.setID = setID;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public String getDataJsonRes() {
        return dataJsonRes;
    }

    public void setDataJsonRes(String dataJsonRes) {
        this.dataJsonRes = dataJsonRes;
    }

    public int getPlaybackCode() {
        return playbackCode;
    }

    public void setPlaybackCode(int playbackCode) {
        this.playbackCode = playbackCode;
    }

}
