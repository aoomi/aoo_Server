package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 赛事动态
 */
@Data
@NoArgsConstructor
public class UnionDynamicItem {
    /**
     * 动态ID
     */
    private long id;
    private Long pid;
    private String clubName;
    private int clubSign;
    private Long clubId;
    private Long execPid;
    private int execTime;
    private int execType;
    private String value;
    private String name;// 所属玩家名称
    private String execName;// 执行操作玩家PID
    private String curValue;
    private String preValue;
    private String roomKey;
    private String msg;
    private String execClubName;
    private int execClubSign;
    //所属玩家当前值
    private String pidCurValue = "";
    //所属玩家前值
    private String pidPreValue = "";
    //所属玩家变化值
    private String pidValue = "";
    //执行玩家当前值
    private String execPidCurValue = "";
    //执行玩家前值
    private String execPidPreValue = "";
    //执行玩家变化值
    private String execPidValue = "";

    public UnionDynamicItem(long id, int clubSign, long pid, long execPid, int execTime, int execType, String value, String name, String execName, String clubName, long clubId, String curValue, String preValue, String roomKey, String msg, String execClubName, int execClubSign, String pidCurValue, String pidPreValue, String pidValue, String execPidCurValue, String execPidPreValue, String execPidValue) {
        this.setId(id);
        this.setClubSign(clubSign);
        this.setPid(pid);
        this.setExecPid(execPid);
        this.setExecTime(execTime);
        this.setExecType(execType);
        this.setValue(value);
        this.setName(name);
        this.setExecName(execName);
        this.setClubName(clubName);
        this.setClubId(clubId);
        this.setCurValue(curValue);
        this.setPreValue(preValue);
        this.setRoomKey(roomKey);
        this.setMsg(msg);
        this.setExecClubName(execClubName);
        this.setExecClubSign(execClubSign);
        this.setPidCurValue(pidCurValue);
        this.setPidPreValue(pidPreValue);
        this.setPidValue(pidValue);
        this.setExecPidCurValue(execPidCurValue);
        this.setExecPidPreValue(execPidPreValue);
        this.setExecPidValue(execPidValue);
    }

    public void setPid(long pid) {
        if (pid <= 0L) {
            return;
        }
        this.pid = pid;
    }

    public void setClubName(String clubName) {
        if (StringUtils.isEmpty(clubName)) {
            return;
        }
        this.clubName = clubName;
    }

    public void setClubId(long clubId) {
        if (clubId <= 0L) {
            return;
        }
        this.clubId = clubId;
    }

    public void setExecPid(long execPid) {
        if (execPid <= 0L) {
            return;
        }
        this.execPid = execPid;
    }

    public void setValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        this.value = value;
    }

    public void setName(String name) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        this.name = name;
    }

    public void setExecName(String execName) {
        if (StringUtils.isEmpty(execName)) {
            return;
        }
        this.execName = execName;
    }

    public void setCurValue(String curValue) {
        if (StringUtils.isEmpty(curValue)) {
            return;
        }
        this.curValue = curValue;
    }

    public void setPreValue(String preValue) {
        if (StringUtils.isEmpty(preValue)) {
            return;
        }
        this.preValue = preValue;
    }

    public void setRoomKey(String roomKey) {
        if (StringUtils.isEmpty(roomKey)) {
            return;
        }
        this.roomKey = roomKey;
    }

    public void setMsg(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }
        this.msg = msg;
    }
}
