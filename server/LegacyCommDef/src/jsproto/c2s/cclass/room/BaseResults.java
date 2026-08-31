package jsproto.c2s.cclass.room;

import lombok.Data;

@Data
public abstract class BaseResults {
    /**
     * 玩家房间内位置
     */
    public int posId = 0;
    /**
     * 玩家PID
     */
    public long pid = 0;
    /**
     * 总积分
     */
    public int point = 0;
    /**
     * 总竞技点分数
     */
    public Double sportsPoint;
    /**
     * 是否房主
     */
    public boolean isOwner = false;
    /**
     * 是否解散发起者
     */
    public Boolean isDissolve;

    /**
     * 解散操作状态（-1:正常结束,0:未操作,1:同意操作,2:拒绝操作,3:发起者）
     */
    public int dissolveState;

    /**
     * 同ip状态（-1:正常结束,0:未操作,1:同意操作,2:拒绝操作,3:相同ip者）
     */
    public int sameIpState;
}
