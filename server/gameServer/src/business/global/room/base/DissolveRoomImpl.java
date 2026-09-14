package business.global.room.base;

import cenum.ClassType;
import cenum.room.DissolveType;
import cenum.room.RoomDissolutionState;
import jsproto.c2s.cclass.room.Room_Dissolve;

public interface DissolveRoomImpl {

    public int getCreatePos();

    /**
     * 操作解散
     *
     * @param pos    操作位置
     * @param agreeD T支持,F拒绝
     * @return
     */
    public boolean deal(int pos, boolean agreeD);

    /**
     * 是否1个人拒绝
     *
     * @return
     */
    public boolean isRefused();

    /**
     * 检查是否同意解散
     *
     * @param type 解散类型
     * @return
     */
    public boolean isAllAgree(DissolveType type);


    /**
     * 是否已超时
     *
     * @param curSec
     * @return
     */
    public boolean isDelay(int curSec);


    /**
     * 结束时间
     *
     * @return
     */
    public int getEndSec();

    /**
     * 剩余时间
     *
     * @return
     */
    public int getLeftSec();

    public Room_Dissolve getNotify();

    public void setRoomDissolutionState(RoomDissolutionState dissolutionState);

    public String getDissolveInfoLog();

    public int reSetDissolve(int playingCount, ClassType classType);
}
