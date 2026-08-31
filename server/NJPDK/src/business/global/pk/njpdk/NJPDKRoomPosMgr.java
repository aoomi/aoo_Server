package business.global.pk.njpdk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.player.Robot.RobotMgr;
import com.ddm.server.common.utils.CommTime;

import java.util.ArrayList;
import java.util.Objects;

/**
 * 安岳一大局位置管理器
 */
public class NJPDKRoomPosMgr extends AbsRoomPosMgr {

    /**
     * 初始化位置
     *
     * @param room
     */
    public NJPDKRoomPosMgr(AbsBaseRoom room) {
        super(room);

    }

    @Override
    protected void initPosList() {
        for (int i = 0; i < getPlayerNum(); i++) {
            posList.add(new NJPDKRoomPos(i, room));
        }
    }

    /**
     * Drive robot and trusteeship actions for the current operation position.
     */
    @Override
    public void checkOverTime(int serverTime) {
        if (serverTime == 0) {
            return;
        }
        for (AbsRoomPos pos : this.getPosList()) {
            if (Objects.isNull(pos) || pos.getPid() <= 0L || pos.getLatelyOutCardTime() <= 0L) {
                continue;
            }
            if (pos.isRobot() && CommTime.nowMS() > pos.getLatelyOutCardTime() + RobotMgr.getInstance().getThinkTime()) {
                pos.setLatelyOutCardTime(CommTime.nowMS());
                this.getRoom().RobotDeal(pos.getPosID());
                continue;
            }
            if (!pos.isTrusteeship() && CommTime.nowMS() > pos.getLatelyOutCardTime() + serverTime) {
                pos.setLatelyOutCardTime(CommTime.nowMS());
                this.getRoom().startTrusteeShipTime();
                pos.setTrusteeship(true, false);
                if (this.getRoom().needAtOnceOpCard()) {
                    this.getRoom().roomTrusteeship(pos.getPosID());
                }
            }
        }
    }

    /**
     * 获取拥有此牌的玩家位置
     *
     * @param card
     * @return
     */
    public int getPosByCard(Integer card) {
        int pos = -1;
        for (AbsRoomPos roomPosDelegateAbstract : posList) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) roomPosDelegateAbstract;
            if (roomPos.checkCard(card)) {
                pos = roomPos.getPosID();
                break;
            }
        }
        return pos;
    }

    /**
     * 获取所有玩家的手牌
     */
    @SuppressWarnings("unchecked")
    public ArrayList<ArrayList<Integer>> getAllPlayBackNotify() {
        ArrayList<ArrayList<Integer>> cardList = new ArrayList<>();
        for (AbsRoomPos roomPosDelegateAbstract : posList) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) roomPosDelegateAbstract;
            cardList.add((ArrayList<Integer>) roomPos.getPrivateCards().clone());
        }
        return cardList;
    }
}
