package business.global.room.base;

import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import jsproto.c2s.cclass.room.Room_ChangePlayerNum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 解散房间
 *
 * @author whx
 **/
@Data
public class ChangePlayerNumRoom {
    public static final int WaitSec = 90;
    public static final int AGREEWAITSEC = 60;
    public int startSec = 0;
    public int createPos = 0;
    protected Map<Integer, Integer> posAgreeList = Maps.newConcurrentHashMap(); // 0未表态
    // 1支持
    // 2拒绝
    public int playerNum;
    public int agreeWaitSec = 0;// 同意一半以上等待时间
    // 改变成几人
    protected int changePlayerNum = -1;
    //房间人数
    protected int setPlayerNum = 4;
    protected AbsBaseRoom room = null;


    public ChangePlayerNumRoom(AbsBaseRoom room, int createPos, int playerNum, int changePlayerNum, int setPlayerNum) {
        this.playerNum = playerNum;
        this.createPos = createPos;
        this.changePlayerNum = changePlayerNum;
        this.startSec = CommTime.nowSecond();
        this.setPlayerNum = setPlayerNum;
        AbsRoomPos roomPos = null;
        for (int i = 0; i < room.getPlayerNum(); i++) {
            roomPos = room.getRoomPosMgr().getPosByPosID(i);
            if (roomPos.getPid() > 0L) {
                posAgreeList.put(i, 0);
            }
        }
        posAgreeList.put(createPos, 1);
        this.room = room;
    }

    public boolean deal(int pos, int agreeD) {
        if (null == posAgreeList.get(pos)) {
            posAgreeList.put(pos, agreeD);
            return false;
        }
        if (null == posAgreeList.get(pos) || (posAgreeList.get(pos) != null && posAgreeList.get(pos) != 0)) {
            return false;
        }
        posAgreeList.put(pos, agreeD);
        return true;
    }

    // 是否1个人拒绝
    public boolean isRefused() {
        for (Integer agreement : posAgreeList.values()) {
            if (agreement == 2) {
                return true;
            }
        }
        return false;
    }

    // 是否3个人同意
    public boolean isAllAgree() {
        int agreeCnt = 0;
        boolean agreeDissolve = false;
        for (Integer agreement : posAgreeList.values()) {
            if (agreement == 1) {
                agreeCnt += 1;
            }
        }
        agreeDissolve = agreeCnt >= playerNum;
        return agreeDissolve;
    }

    public int notOpPos() {
        for (Map.Entry<Integer, Integer> entry : posAgreeList.entrySet()) {
            if (entry.getValue() == 0) {
                return entry.getKey();
            }
        }
        return -1;
    }

    // 是否已超时
    public boolean isDelay(int curSec) {
        return curSec >= this.startSec + WaitSec;
    }

    // 结束时间
    public int getEndSec() {
        return this.startSec + WaitSec;
    }

    // 剩余时间
    public int getLeftSec() {
        return Math.max(0, this.startSec + WaitSec - CommTime.nowSecond());
    }

    /**
     * 房间人数切换通知
     *
     * @param isChangePos true:是切换位置的状态 false:不是切换位置的状态
     * @return
     */
    public Room_ChangePlayerNum getNotify(boolean isChangePos) {
        Room_ChangePlayerNum ret = new Room_ChangePlayerNum();
        ret.setCreatePos(this.getCreatePos());
        ret.setPosAgreeList(this.getPosAgreeList(isChangePos));
        ret.setEndSec(this.getEndSec());
        return ret;
    }

    /**
     * @return posAgreeList
     */
    public List<Integer> getPosAgreeList(boolean isChangePos) {
        if (isChangePos) {
            List<Integer> agreeList = new ArrayList<>();
            for (int i = 0; i < setPlayerNum; i++) {
                AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(i);
                if (roomPos != null && roomPos.getPlayer() != null && posAgreeList.get(i) == null) {
                    room.getOpChangePlayerRoom().changePlayerNumAgree(roomPos.getPlayer().getPid(), 2);
                }
                agreeList.add(Optional.ofNullable(posAgreeList.get(i)).orElse(0));
            }
            return agreeList;
        }
        return new ArrayList<Integer>(posAgreeList.values());
    }

    /**
     * 检查同意时间
     */
    public boolean chcekAgreeWaitSec() {
        return CommTime.nowSecond() > this.agreeWaitSec + AGREEWAITSEC;
    }

    /**
     * 清空
     */
    public void clear() {
        this.posAgreeList = null;
    }
}
