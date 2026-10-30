package business.global.room.base;

import business.global.shareplayback.SharePlayBackKeyMgr;
import cenum.PrizeType;
import cenum.room.SetState;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.GameSetBO;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.playback.PlayBackDateTimeInfo;
import jsproto.c2s.cclass.room.RoomSetEndInfo;
import jsproto.c2s.cclass.room.RoomSetInfo;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.Objects;

@Data
public abstract class AbsRoomSet {
    /**
     * 数据库
     */
    private GameSetBO bo = null;
    /**
     * 当前第几局
     */
    private int setID = 0;
    /**
     * 开局时间
     */
    protected long startMS = 0;
    /**
     * 是否当局结束
     */
    private boolean isEnd = false;

    /**
     * 当局结算时间
     */
    private int curSetEndTime = 0;

    /**
     * 存放回放时间和key等信息
     */
    private PlayBackDateTimeInfo playBackDateTimeInfo;

    /**
     * 当局状态
     */
    protected SetState state = SetState.Init;

    public void reInit() {

    }

    public AbsRoomSet(int setID) {
        super();
        // 设置局数ID
        this.setID = setID;
    }

    public void setEnd(boolean end) {
        if (end) {
            // 当局结算时间
            this.setCurSetEndTime(CommTime.nowSecond());
        }
        this.isEnd = end;
    }

    public boolean isEnd() {
        return isEnd;
    }

    /**
     * 检查是否存在指定消耗类型
     *
     * @return
     */
    public abstract boolean checkExistPrizeType(PrizeType prizeType);

    /**
     * 当局更新
     *
     * @param sec 秒
     * @return
     */
    public abstract boolean update(int sec);

    /**
     * 清空数据
     */
    public abstract void clear();

    /**
     * 清空BO数据
     */
    public abstract void clearBo();

    /**
     * 获取当局信息
     */
    public abstract RoomSetInfo getNotify_set(long pid);

    /**
     * 获取当局结算信息
     *
     * @return
     */
    public abstract RoomSetEndInfo getNotify_setEnd();

    /**
     * 本局数据
     *
     * @return
     */
    public GameSetBO getBo() {
        if (Objects.isNull(this.bo)) {
            this.bo = new GameSetBO();
        }
        return bo;
    }

    /**
     * 本局数据信息
     *
     * @param bo
     */
    public void setBo(GameSetBO bo) {
        this.bo = bo;
    }

    /**
     * 当局ID
     *
     * @return
     */
    public int getSetID() {
        return setID;
    }

    public long getStartMS() {
        return startMS;
    }

    public void setStartMS(long startMS) {
        this.startMS = startMS;
    }

    /**
     * 结算
     */
    public abstract void endSet();

    /**
     * 回放记录谁发起解散
     *
     * @param baseSendMsg
     */
    public abstract void addDissolveRoom(BaseSendMsg baseSendMsg);

    /**
     * 回放记录添加游戏配置
     */
    public abstract void addGameConfig();

    /**
     * 标识Id
     *
     * @return
     */
    public abstract int getTabId();

    /**
     * 存放回放时间和key等信息
     *
     * @return
     */
    public PlayBackDateTimeInfo getPlayBackDateTimeInfo() {
        if (Objects.isNull(this.playBackDateTimeInfo)) {
            int code = SharePlayBackKeyMgr.getInstance().getNewKey().intValue();
            DateTime nowTime = DateTime.now();
            int playBackCode = Integer.parseInt(String.format("%d%d", nowTime.getDayOfWeek(), code));
            this.setPlayBackDateTimeInfo(new PlayBackDateTimeInfo(code, nowTime.getDayOfWeek(), String.format("%s0%d", nowTime.toString("yyyyMMdd"), nowTime.getDayOfWeek()), playBackCode, getTabId()));
        }
        return this.playBackDateTimeInfo;
    }

    /**
     * 计算大局分数
     */
    public void calcDaJuFenShuRoomPoint() {
    }

    /**
     * 发牌的时候记录分数
     * 只赢当前身上分的时候要用
     */
    public void recordRoomPosPointBeginStart() {
    }

    /**
     * 检测是否需要解散房间
     *
     * @return
     */
    public boolean checkNeedEndRoom() {
        return false;
    }
}
