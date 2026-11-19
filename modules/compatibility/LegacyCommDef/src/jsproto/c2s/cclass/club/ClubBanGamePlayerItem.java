package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubBanGamePlayerItem implements Serializable {

    private long pid;
    private String name = "";
    private String headImageUrl = "";
    private long clubId;
    private int createTime;
    private String deletePidName;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadImageUrl() {
        return headImageUrl;
    }

    public void setHeadImageUrl(String headImageUrl) {
        this.headImageUrl = headImageUrl;
    }

    public long getClubId() {
        return clubId;
    }

    public void setClubId(long clubId) {
        this.clubId = clubId;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public String getDeletePidName() {
        return deletePidName;
    }

    public void setDeletePidName(String deletePidName) {
        this.deletePidName = deletePidName;
    }

    public static String getItemsName() {
        return "pid as pid,name as name,headImageUrl as headImageUrl,clubId as clubId,createTime as createTime,deletePidName as deletePidName";
    }
}
