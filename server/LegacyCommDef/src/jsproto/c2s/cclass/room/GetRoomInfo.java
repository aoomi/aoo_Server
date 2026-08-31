package jsproto.c2s.cclass.room;

import cenum.PrizeType;
import cenum.room.RoomState;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 房间公共信息
 *
 * @param <T>
 * @author Administrator
 */
public class GetRoomInfo<T> extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 房间Key
    private String key;
    // 创建时间
    private int createSec;
    // 消费类型
    private PrizeType prizeType;
    // 房间状态
    private RoomState state;
    // 当局局数
    private int setID;
    // 房间创建者
    private long ownerID;
    // 房间玩家信息列表
    private List<RoomPosInfo> posList;
    // 房间解散信息
    private Room_Dissolve dissolve;
    // 环信语音
    private RoomHXSDKChatInfo chatInfo;
    // 创建类型
    private int createType = 0;
    // 比赛场配置
    private ArenaRoomConfig arenaCfg;
    // 亲友圈配置
    private ClubRoomConfig clubCfg;
    // 赛事房间配置
    private UnionRoomConfig unionCfg = null;
    // 游戏配置
    private T cfg;
    // 房间交换玩家人数
    private Room_ChangePlayerNum changePlayerNum;

    private int roomType;

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getCreateSec() {
        return createSec;
    }

    public void setCreateSec(int createSec) {
        this.createSec = createSec;
    }

    public PrizeType getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(PrizeType prizeType) {
        this.prizeType = prizeType;
    }

    public RoomState getState() {
        return state;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public int getSetID() {
        return setID;
    }

    public void setSetID(int setID) {
        this.setID = setID;
    }

    public long getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(long ownerID) {
        this.ownerID = ownerID;
    }

    public List<RoomPosInfo> getPosList() {
        return posList;
    }

    public void setPosList(List<RoomPosInfo> posList) {
        this.posList = posList;
    }

    public Room_Dissolve getDissolve() {
        return dissolve;
    }

    public void setDissolve(Room_Dissolve dissolve) {
        this.dissolve = dissolve;
    }

    public RoomHXSDKChatInfo getChatInfo() {
        return chatInfo;
    }

    public void setChatInfo(RoomHXSDKChatInfo chatInfo) {
        this.chatInfo = chatInfo;
    }

    public int getCreateType() {
        return createType;
    }

    public void setCreateType(int createType) {
        this.createType = createType;
    }

    public ArenaRoomConfig getArenaCfg() {
        return arenaCfg;
    }

    public void setArenaCfg(ArenaRoomConfig arenaCfg) {
        this.arenaCfg = arenaCfg;
    }

    public ClubRoomConfig getClubCfg() {
        return clubCfg;
    }

    public void setClubCfg(ClubRoomConfig clubCfg) {
        this.clubCfg = clubCfg;
    }

    public T getCfg() {
        return cfg;
    }

    public void setCfg(T cfg) {
        this.cfg = cfg;
    }

    public Room_ChangePlayerNum getChangePlayerNum() {
        return changePlayerNum;
    }

    public void setChangePlayerNum(Room_ChangePlayerNum changePlayerNum) {
        this.changePlayerNum = changePlayerNum;
    }

    public UnionRoomConfig getUnionCfg() {
        return unionCfg;
    }

    public void setUnionCfg(UnionRoomConfig unionCfg) {
        this.unionCfg = unionCfg;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }
}