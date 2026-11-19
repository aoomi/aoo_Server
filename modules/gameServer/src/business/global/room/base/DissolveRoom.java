package business.global.room.base;

import cenum.ClassType;
import cenum.room.DissolveType;
import cenum.room.RoomDissolutionState;
import com.ddm.server.common.utils.CommTime;
import jsproto.c2s.cclass.room.Room_Dissolve;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解散房间
 *
 * @author Administrator
 */
@Data
public class DissolveRoom implements DissolveRoomImpl {
    public static final int PK_TIME = 60;
    public static final int MJ_TIME = 30;
    /**
     * 等待时间
     */
    private int WaitSec = 90;
    /**
     * 开始时间
     */
    private int startSec = 0;
    /**
     * 解散发起者
     */
    private int createPos = 0;
    /**
     * 解散表态列表 0未表态 1支持 2拒绝
     */
    private List<Integer> posAgreeList = new ArrayList<>();
    /**
     * 房间信息
     */
    private AbsBaseRoom room;
    /**
     * 房间解散状态
     */
    private RoomDissolutionState roomDissolutionState = RoomDissolutionState.Normal;

    public DissolveRoom(AbsBaseRoom room, int createPos, int WaitSec) {
        this.room = room;
        this.WaitSec = WaitSec;
        this.createPos = createPos;
        this.startSec = CommTime.nowSecond();
        for (int i = 0; i < this.getRoom().getPlayerNum(); i++) {
            posAgreeList.add(0);
        }
        posAgreeList.set(createPos, 1);
    }

    public void clear() {
        this.room = null;
        this.posAgreeList = null;
    }

    @Override
    public int getCreatePos() {
        return createPos;
    }

    /**
     * 重新设置解散时间
     */
    @Override
    public int reSetDissolve(int playingCount, ClassType classType) {
        if (this.getRoom().isReSetDissolve()) {
            // 同意次数 >= 总人数 -1,重新计时。
            int agreeCount = (int) this.posAgreeList.stream().filter(k -> k == 1).count();
            if (agreeCount >= (playingCount - 1)) {
                int sec = ClassType.MJ.equals(classType) ? MJ_TIME : PK_TIME;
                if (getLeftSec() > sec) {
                    this.WaitSec = sec;
                    this.startSec = CommTime.nowSecond();
                }
            }
        }
        return this.getEndSec();
    }

    /**
     * 操作解散
     *
     * @param pos    操作位置
     * @param agreeD T支持,F拒绝
     * @return
     */
    @Override
    public boolean deal(int pos, boolean agreeD) {
        if (posAgreeList.get(pos) != 0) {
            return false;
        }
        posAgreeList.set(pos, agreeD ? 1 : 2);
        return true;
    }

    /**
     * 获取位置的解散操作状态
     *
     * @return 解散操作状态（-1:正常结束,0:未操作,1:同意操作,2:拒绝操作,3:发起者）
     */
    public int getDissolveState(int pos) {
        if (pos == this.getCreatePos()) {
            return 3;
        }
        return posAgreeList.get(pos);
    }

    /**
     * 是否1个人拒绝
     *
     * @return
     */
    @Override
    public boolean isRefused() {
        return posAgreeList.contains(2);
    }

    /**
     * 检查是否同意解散
     *
     * @param type 解散类型
     * @return
     */
    @Override
    public boolean isAllAgree(DissolveType type) {
        if (DissolveType.ALL.equals(type)) {
            // 全部同意才解散。
            if (this.checkAllAgree()) {
                // 记录解散状态
                this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
                return true;
            }
        } else {
            // 超过一半同意才能解散。
            if (this.checkHalfAgree()) {
                // 记录解散状态
                this.setRoomDissolutionState(RoomDissolutionState.Dissolution);
                return true;
            }
        }
        return false;
    }

    /**
     * 全部同意才解散。
     *
     * @return
     */
    private boolean checkAllAgree() {
        int agreeCnt = (int) this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> null != k && k.isPlayTheGame() && this.posAgreeList.get(k.getPosID()) == 1).count();
        return agreeCnt >= this.room.getPlayingCount();
    }

    /**
     * 超过一半同意才能解散。
     *
     * @return
     */
    private boolean checkHalfAgree() {
        boolean agreeDissolve = false;
        int agreeCnt = (int) this.getRoom().getRoomPosMgr().getPosList().stream().filter(k -> null != k && k.isPlayTheGame() && this.posAgreeList.get(k.getPosID()) == 1).count();
        if (this.room.getPlayingCount() > 2 && agreeCnt >= Math.ceil((this.room.getPlayingCount() + 1) / 2.0)) {
            agreeDissolve = true;
        } else {
            agreeDissolve = agreeCnt >= this.room.getPlayingCount();
        }
        return agreeDissolve;
    }


    /**
     * 是否已超时
     *
     * @param curSec
     * @return
     */
    @Override
    public boolean isDelay(int curSec) {
        return curSec >= this.startSec + WaitSec;
    }

    public void setWaitSec(int waitSec) {
        WaitSec = waitSec;
    }

    /**
     * 结束时间
     *
     * @return
     */
    @Override
    public int getEndSec() {
        return this.startSec + WaitSec;
    }

    /**
     * 剩余时间
     *
     * @return
     */
    @Override
    public int getLeftSec() {
        return Math.max(0, this.startSec + WaitSec - CommTime.nowSecond());
    }

    @Override
    public Room_Dissolve getNotify() {
        Room_Dissolve ret = new Room_Dissolve();
        ret.setCreatePos(this.createPos);
        ret.setPosAgreeList(this.posAgreeList);
        ret.setEndSec(this.getEndSec());
        return ret;
    }

    /**
     * @return posAgreeList
     */
    public List<Integer> getPosAgreeList() {
        return posAgreeList;
    }

    /**
     * 获取房间信息
     *
     * @return
     */
    public AbsBaseRoom getRoom() {
        return room;
    }

    @Override
    public String getDissolveInfoLog() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startSec").append(":").append(startSec);
        stringBuilder.append(",createPos").append(":").append(createPos);
        stringBuilder.append(",posAgreeList").append(":").append(posAgreeList.toString());
        return stringBuilder.toString();
    }

    @Override
    public void setRoomDissolutionState(RoomDissolutionState roomDissolutionState) {
        this.roomDissolutionState = roomDissolutionState;
    }
}
