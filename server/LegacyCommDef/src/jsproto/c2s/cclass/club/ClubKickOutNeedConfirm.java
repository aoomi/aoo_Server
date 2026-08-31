package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubKickOutNeedConfirm {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 弹窗类型
     * 0 无弹窗
     * 1 有保险箱分数不为0弹窗
     */
    private int type;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ClubKickOutNeedConfirm() {
    }


}
