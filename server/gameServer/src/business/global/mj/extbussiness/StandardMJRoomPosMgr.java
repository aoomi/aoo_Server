package business.global.mj.extbussiness;

import business.global.mj.extbussiness.dto.StandardMJWaitingExInfo;
import business.global.room.base.AbsBaseRoom;
import business.global.room.mj.MJRoomPosMgr;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.ddm.server.common.utils.CommTime;

/**
 * 麻将 房间内每个位置信息
 *
 * @author Clark
 * @apiNote 子游戏请继承这个类
 */
public class StandardMJRoomPosMgr extends MJRoomPosMgr {

    public StandardMJRoomPosMgr(AbsBaseRoom room) {
        super(room);
    }

    @Override
    protected void initPosList() {
        for (int posID = 0; posID < this.getPlayerNum(); posID++) {
            this.posList.add(new StandardMJRoomPos(posID, room));
        }
    }

    @Override
    public boolean isAllContinue() {
        int time = getTimeOutContinueSec();
        this.getPosList().forEach(roomPos -> {
            if (time > 0 && roomPos.getPid() > 0 && !roomPos.isGameReady()
                    && roomPos.getTimeSec() > 0
                    && CommTime.nowSecond() - roomPos.getTimeSec() >= time) {
                getRoom().continueGame(roomPos.getPid());
            }
        });
        return super.isAllContinue();
    }

    public boolean checkAllOpPiaoFen() {
        return this.getPosList().stream().allMatch(roomPos -> ((StandardMJRoomPos) roomPos).getPiaoFenEnum().getPiao() >= 0);
    }

    public ArrayList<StandardMJWaitingExInfo> getPiaoFenInfoList() {
        return new ArrayList<>(this.getPosList().stream().map(roomPos -> ((StandardMJRoomPos) roomPos).getPiaoFenEnum()).collect(Collectors.toList()));
    }

    public int getTimeOutContinueSec() {
        int adapterFangJian = ((StandardMJRoom) room).getAdapterFangJian(StandardMJRoomEnum.GameRoomConfigEnum.XiaoJu10Miao.name());
        return room.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(adapterFangJian) ? 10 : 0;
    }
}
