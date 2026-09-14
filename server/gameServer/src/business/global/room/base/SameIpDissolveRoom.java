package business.global.room.base;

import cenum.room.RoomDissolutionState;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.common.utils.Maps;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 解散房间
 *
 * @author Administrator
 */
@Data
public class SameIpDissolveRoom {
    /**
     * 等待时间
     */
    private int WaitSec = 90;
    /**
     * 开始时间
     */
    private int startSec = 0;
    /**
     * 解散表态列表 0未表态 1支持 2拒绝
     */
    private Map<Long, Integer> posAgreeMap = Maps.newMap();
    /**
     * 房间信息
     */
    private AbsBaseRoom room;
    /**
     * 房间解散状态
     */
    private RoomDissolutionState roomDissolutionState = RoomDissolutionState.Normal;

    private List<Long> sameIpPidList = Lists.newArrayList();

    public SameIpDissolveRoom(AbsBaseRoom room, List<Long> sameIpPidList, int WaitSec) {
        this.room = room;
        this.WaitSec = WaitSec;
        this.sameIpPidList = sameIpPidList;
        this.startSec = CommTime.nowSecond();
        for (Long pid : this.getRoom().getRoomPosMgr().getRoomPidAll()) {
            if (sameIpPidList.contains(pid)) {
                posAgreeMap.put(pid, 3);
            } else {
                posAgreeMap.put(pid, 0);
            }
        }
    }

    public void clear() {
        this.room = null;
        this.posAgreeMap = null;
        this.sameIpPidList = null;
    }


    /**
     * 操作解散
     *
     * @param pid    玩家pid
     * @param agreeD T支持,F拒绝
     * @return
     */
    public boolean deal(long pid, boolean agreeD) {
        int value = Optional.ofNullable(this.posAgreeMap.get(pid)).orElseGet(() -> 2);
        if (value != 0) {
            return false;
        }
        posAgreeMap.put(pid, agreeD ? 1 : 2);
        return agreeD;
    }


    /**
     * 是否已超时
     *
     * @param curSec
     * @return
     */
    public boolean isDelay(int curSec) {
        return curSec >= this.startSec + WaitSec;
    }

    /**
     * 结束时间
     *
     * @return
     */
    public int getEndSec() {
        return this.startSec + WaitSec;
    }

    /**
     * 剩余时间
     *
     * @return
     */
    public int getLeftSec() {
        return Math.max(0, this.startSec + WaitSec - CommTime.nowSecond());
    }

    /**
     * 获取房间信息
     *
     * @return
     */
    public AbsBaseRoom getRoom() {
        return room;
    }

    /**
     * 获取位置的解散操作状态
     *
     * @return 解散操作状态（-1:正常结束,0:未操作,1:同意操作,2:拒绝操作,3:相同ip者）
     */
    public int getDissolveState(long pid) {
        return Optional.ofNullable(this.posAgreeMap.get(pid)).orElseGet(() -> 0);
    }

}
