package business.global.room.base;

import business.global.room.PlayBackMgr;
import cenum.PrizeType;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import core.db.entity.clarkGame.PlayerPlayBackBO;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.playback.PlayBackEvent;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.playback.PlayBackData;
import jsproto.c2s.cclass.playback.PlayBackList;
import jsproto.c2s.cclass.playback.PlayBackMsg;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 回放的房间管理
 *
 * @author zaf
 */
@Data
public abstract class RoomPlayBackImplAbstract implements RoomPlayBack {
    //每局回放记录-临时存储
    @SuppressWarnings("rawtypes")
    private List<PlayBackMsg> playBackMsgs = new CopyOnWriteArrayList<>();
    private PlayerPlayBackBO playBackBO = null;
    private Gson gson = null;
    private AbsBaseRoom room;
    private PrizeType prizeType = PrizeType.RoomCard;

    public RoomPlayBackImplAbstract(AbsBaseRoom room) {
        this.room = room;
        this.playBackBO = new PlayerPlayBackBO();
        this.gson = new Gson();
        this.setPrizeType(this.getRoom().getBaseRoomConfigure().getPrizeType());
    }


    @Override
    public void addPlayBack(PlayBackData playBackData) {
        if (!isRoomCard()) {
            return;
        }
        playBackBO.setEndTime(CommTime.nowSecond());
        PlayBackList pBackList = new PlayBackList(getPlaybackList());
        playBackBO.setPlayBackRes(new StringBuilder(this.gson.toJson(pBackList)));
        playBackBO.setPlayerList(this.gson.toJson(this.getRoom().getRoomPosMgr().getRoomPlayerPosList()));
        playBackBO.setRoomID(playBackData.getRoomID());
        playBackBO.setRoomKey(playBackData.getRoomKey());
        playBackBO.setSetCount(playBackData.getCount());
        playBackBO.setSetID(playBackData.getSetID());
        playBackBO.setDPos(playBackData.getDPos());
        playBackBO.setPlayBackCode(playBackData.getPlayBackDateTimeInfo().getPlayBackCode());
        playBackBO.setTabId(playBackData.getPlayBackDateTimeInfo().getTabId());
        playBackBO.setGameType(playBackData.getGameType());
        DispatcherComponent.getInstance().publish(new PlayBackEvent(PlayBackMgr.getInstance().path(playBackData.getPlayBackDateTimeInfo()), playBackBO));
        clearPlayBack();
    }

    /**
     * 清空回放
     */
    public void clearPlayBack() {
        if (CollectionUtils.isNotEmpty(playBackMsgs)) {
            // 清空回放数据
            this.playBackMsgs.clear();
        }
    }

    @Override
    public void clear() {
        if (null != this.playBackBO) {
            this.playBackBO = null;
        }
        // 清空回放数据
        if (null != this.playBackMsgs) {
            this.playBackMsgs.forEach(key -> {
                if (null != key) {
                    key.clean();
                }
            });
            this.playBackMsgs.clear();
            this.playBackMsgs = null;
        }
        this.room = null;
        this.gson = null;
    }


    /**
     * 通知所有人
     *
     * @param <T>
     * @param msg
     */
    @Override
    public <T> void playBack2All(BaseSendMsg msg) {
        // 通知指定位置的人
        this.getRoom().getRoomPosMgr().notify2All(msg);
        addPlaybackList(msg, null);
        business.global.replay.LegacyPerspectiveReplayRecorder.getInstance().recordPublic(
                this.getRoom().getRoomID(), this.getRoom().getCurSetID(), msg);

    }

    /**
     * 通知指定位置的人
     *
     * @param <T>
     * @param pos
     * @param msg
     */
    @Override
    public <T> void playBack2Pos(int pos, BaseSendMsg msg, List<T> setPosCard) {
        // 通知指定位置的人
        this.getRoom().getRoomPosMgr().notify2Pos(pos, msg);
        var roomPos = this.getRoom().getRoomPosMgr().getPosByPosID(pos);
        if (roomPos != null) {
            business.global.replay.LegacyPerspectiveReplayRecorder.getInstance().recordPrivate(
                    this.getRoom().getRoomID(), this.getRoom().getCurSetID(), roomPos.getPid(), msg);
        }
        // Targeted messages may contain private cards and must not enter shared replay.
    }


    private boolean isRoomCard() {
        if (PrizeType.Gold.equals(this.getPrizeType())) {
            return false;
        }
        return true;
    }


    @SuppressWarnings("rawtypes")
    private List<PlayBackMsg> getPlaybackList() {
        return playBackMsgs;
    }

    /**
     * 检查命令是否是牌的操作
     **/
    public abstract boolean isOpCard(BaseSendMsg msg);

    @Override
    public <T> void addPlaybackList(BaseSendMsg msg, List<T> setPosCard) {
        if (!isRoomCard()) {
            return;
        }
        if (null != this.playBackMsgs) {
            if (this.isOpCard(msg)) {
                this.playBackMsgs.add(new PlayBackMsg<T>(msg.getOpName(), msg, setPosCard));
            } else {
                this.playBackMsgs.add(new PlayBackMsg<T>(msg.getOpName(), msg, null));
            }
        }
    }


    /**
     * 获取房间信息
     *
     * @return
     */
    public AbsBaseRoom getRoom() {
        return room;
    }

}
