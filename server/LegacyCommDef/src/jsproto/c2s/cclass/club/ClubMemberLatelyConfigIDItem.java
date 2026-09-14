package jsproto.c2s.cclass.club;

import lombok.Data;


/**
 * 只查询Id
 */
@Data
public class ClubMemberLatelyConfigIDItem {
    private long id;

    private long configID;

    private long memberID;
    private long unionID;
    private long clubID;
    private long updateTime;
    private long startTime;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getConfigID() {
        return configID;
    }

    public void setConfigID(long configID) {
        this.configID = configID;
    }

    public long getMemberID() {
        return memberID;
    }

    public void setMemberID(long memberID) {
        this.memberID = memberID;
    }

    public long getUnionID() {
        return unionID;
    }

    public void setUnionID(long unionID) {
        this.unionID = unionID;
    }

    public long getClubID() {
        return clubID;
    }

    public void setClubID(long clubID) {
        this.clubID = clubID;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public static String getItemsNameUid() {
        return "id,configID,memberID,unionID,clubID,updateTime,startTime";
    }


}
