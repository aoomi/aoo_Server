package jsproto.c2s.cclass.union;

import lombok.Data;

import java.io.Serializable;

@Data
public class UnionBanGamePlayerItem implements Serializable {

    private long pid;
    private String name = "";
    private String headImageUrl = "";
    private long unionId;
    private int createTime;

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

    public long getUnionId() {
        return unionId;
    }

    public void setUnionId(long unionId) {
        this.unionId = unionId;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public static String getItemsName() {
        return "pid as pid,name as name,headImageUrl as headImageUrl,unionId as unionId,createTime as createTime";
    }
}
