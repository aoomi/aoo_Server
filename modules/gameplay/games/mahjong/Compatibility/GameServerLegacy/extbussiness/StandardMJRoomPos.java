package business.global.mj.extbussiness;

import business.global.mj.extbussiness.dto.StandardMJRoomPosInfo;
import business.global.mj.extbussiness.dto.StandardMJWaitingExInfo;
import business.global.room.base.AbsBaseRoom;
import business.global.room.mj.MJRoomPos;

/**
 * 麻将房间内每个位置信息
 *
 * @author Huaxing
 * @apiNote 子游戏请继承这个类
 */
public class StandardMJRoomPos extends MJRoomPos {

    private int tuoGuanSetCount = 0; // 连续托管局数

    /**
     * 飘等阶段
     */
    private StandardMJWaitingExInfo waitingExInfo;

    public StandardMJRoomPos(int posID, AbsBaseRoom room) {
        super(posID, room);
        this.waitingExInfo = new StandardMJWaitingExInfo(posID);
    }


    public int getTuoGuanSetCount() {
        return tuoGuanSetCount;
    }

    /**
     * 增加托管局数
     */
    public void addTuoGuanSetCount() {
        tuoGuanSetCount += 1;
    }

    /**
     * 连续托管局数清零
     */
    public void clearTuoGuanSetCount() {
        tuoGuanSetCount = 0;
    }

    /**
     * 设置托管状态
     *
     * @param isTrusteeship 托管状态
     * @param isOwn         是否屏蔽自己
     */
    public void setTrusteeship(boolean isTrusteeship, boolean isOwn) {
        if (this.isTrusteeship() == isTrusteeship) {
            return;
        }
        // 托管2小局解散：连续2局托管
        if (!isTrusteeship) { // 玩家取消托管，连续托管局数清零
            this.clearTuoGuanSetCount();
        } else {
            this.addTuoGuanSetCount();
        }
        this.setTrusteeship(isTrusteeship);
        if (isOwn) {
            this.getRoom().getRoomPosMgr().notify2ExcludePosID(this.getPosID(), this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
        } else {
            this.getRoom().getRoomPosMgr().notify2All(this.getRoom().Trusteeship(this.getRoom().getRoomID(), this.getPid(), this.getPosID(), this.isTrusteeship()));
        }
    }

    /**
     * @return m_piaoFenEnum
     */
    public StandardMJWaitingExInfo getPiaoFenEnum() {
        return waitingExInfo;
    }

    public void setPiaoFenEnum(StandardMJWaitingExInfo waitingExInfo) {
        this.waitingExInfo = waitingExInfo;
    }

    /**
     * 位置信息适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     */
    public StandardMJRoomPosInfo newRoomPosInfo() {
        return new StandardMJRoomPosInfo();
    }
}

