package business.global.mj.scjymj;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.scjymj.SCJYMJRoomEnum.*;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.global.room.mj.MahjongRoom;
import business.scjymj.c2s.cclass.SCJYMJRoomSetInfo;
import business.scjymj.c2s.iclass.*;
import cenum.ChatType;
import cenum.ClassType;
import cenum.room.GaoJiTypeEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.S_GetRoomInfo;
import jsproto.c2s.iclass.room.SBase_Dissolve;
import jsproto.c2s.iclass.room.SBase_PosLeave;

import java.util.List;

/**
 * 江苏扬中麻将 房间逻辑
 *
 * @param <T>
 * @author Huaxing
 */

public class SCJYMJRoom<T> extends MahjongRoom {
    public CSCJYMJ_CreateRoom cfg;// 开房配置
    public List<Integer> kexuanwanfa;

    protected SCJYMJRoom(BaseRoomConfigure<CSCJYMJ_CreateRoom> baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
        initShareBaseCreateRoom(CSCJYMJ_CreateRoom.class, baseRoomConfigure);
        this.cfg = (CSCJYMJ_CreateRoom) this.getBaseRoomConfigure().getBaseCreateRoom();
        this.kexuanwanfa = this.cfg.getKexuanwanfa();
    }

    /**
     * 获取房间配置
     *
     * @return
     */
    public CSCJYMJ_CreateRoom getRoomCfg() {
        if (this.cfg == null) {
            initShareBaseCreateRoom(CSCJYMJ_CreateRoom.class, getBaseRoomConfigure());
            return (CSCJYMJ_CreateRoom) getBaseRoomConfigure().getBaseCreateRoom();
        }
        return this.cfg;
    }

    @Override
    public double initMinPoint() {
        return 0;
    }

    /**
     * 自动开始游戏 所有玩家准备好自动开始。
     */
    @Override
    public boolean autoStartGame() {
        return true;
    }

    @Override
    public boolean autoReadyGame() {
        return getOther(SCJYMJQiTa.ZiDong);
    }

    /**
     * 房主是否需要准备
     *
     * @return
     */
    @Override
    public boolean ownerNeedReady() {
        return true;
    }

    @Override
    public String dataJsonCfg() {
        return new Gson().toJson(this.getRoomCfg());
    }

    @Override
    public <E> boolean RoomCfg(E m) {
        SCJYMJCfg cfgEnum = (SCJYMJCfg) m;
        int cfgInt = cfgEnum.ordinal();
        if (this.kexuanwanfa.contains(cfgInt)) {
            return true;
        }
        return false;
    }

    @Override
    public int getWanfa() {
        return this.cfg.getWanfa();
    }

    /**
     * 2房牌,3房牌
     *
     * @return
     */
    public SCJYMJFangZhang getFangZhang() {
        return SCJYMJFangZhang.valueOf(this.cfg.paishu);
    }


    /**
     * 2番,3番，4番
     *
     * @return
     */
    public SCJYMJFengDing getFengDing() {
        return SCJYMJFengDing.valueOf(this.cfg.fengDing);
    }

    /**
     * 自动准备,庄闲玩法
     *
     * @return
     */
    public boolean getOther(SCJYMJQiTa qiTa) {
        return this.getRoomCfg().other.contains(qiTa.ordinal());
    }

    /**
     * 自摸加番,自摸加底
     *
     * @return
     */
    public SCJYMJHuPai getHuType() {
        return SCJYMJHuPai.valueOf(this.getRoomCfg().hutype);
    }

    /**
     *  飘：飘在内（选飘）、飘在外（选飘）飘在内（座飘）、飘在外（座飘）、不飘
     *
     * @return
     */
    public SCJYMJPiaoWanFa getPiaoWanFa() {
        return SCJYMJPiaoWanFa.valueOf(this.getRoomCfg().piao);
    }

    /**
     * 托管时间值
     *
     * @return
     */
    @Override
    public int trusteeshipTimeValue() {
        return SCJYMJXianShi.valueOf(this.getRoomCfg().getXianShi()).value();
    }

    /**
     * 开始新一局
     */
    @Override
    public void startNewSet() {
        this.setCurSetID(this.getCurSetID() + 1);
        // / 计算庄位
        if (this.getCurSetID() == 1) {
            setDPos(0);
        } else if (this.getCurSet() != null) {
            AbsMJSetRoom mRoomSet = (AbsMJSetRoom) this.getCurSet();
            // 根据上一局计算下一局庄家
            setDPos(mRoomSet.calcNextDPos());
            mRoomSet.clear();
        }
        // 每个位置，清空准备状态
        this.getRoomPosMgr().clearGameReady();
        this.getRoomTyepImpl().roomSetIDChange();
        this.setCurSet(this.newMJRoomSet(this.getCurSetID(), this, this.getDPos()));
    }

    @Override
    public boolean isCanChangePlayerNum() {
        return false;
    }

    @Override
    protected AbsMJSetRoom newMJRoomSet(int curSetID, MahjongRoom room, int dPos) {
        return new SCJYMJRoomSet(curSetID, room, dPos);
    }

    @Override
    public int getTimerTime() {
        return 200;
    }

    @Override
    public BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship) {
        return SSCJYMJ_Trusteeship.make(roomID, pid, pos, trusteeship);
    }

    @Override
    public BaseSendMsg PosLeave(SBase_PosLeave posLeave) {
        return SSCJYMJ_PosLeave.make(posLeave);
    }

    @Override
    public BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect, boolean isShowLeave) {
        return SSCJYMJ_LostConnect.make(roomID, pid, isLostConnect, isShowLeave);
    }

    @Override
    public BaseSendMsg PosContinueGame(long roomID, int pos) {
        return SSCJYMJ_PosContinueGame.make(roomID, pos);
    }

    @Override
    public BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom) {
        return SSCJYMJ_PosUpdate.make(roomID, pos, posInfo, custom);
    }

    @Override
    public BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady) {
        return SSCJYMJ_PosReadyChg.make(roomID, pos, isReady);
    }

    @Override
    public BaseSendMsg Dissolve(SBase_Dissolve dissolve) {
        return SSCJYMJ_Dissolve.make(dissolve);
    }

    @Override
    public BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec) {
        return SSCJYMJ_StartVoteDissolve.make(roomID, createPos, endSec);
    }

    @Override
    public BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve, int endSec) {
        return SSCJYMJ_PosDealVote.make(roomID, pos, agreeDissolve);
    }

    @Override
    public BaseSendMsg Voice(long roomID, int pos, String url) {
        return SSCJYMJ_Voice.make(roomID, pos, url);
    }

    @Override
    public BaseSendMsg XiPai(long roomID, long pid, ClassType cType) {
        return SSCJYMJ_XiPai.make(roomID, pid, cType);
    }

    @Override
    public BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId, int quickID) {
        return SSCJYMJ_ChatMessage.make(pid, name, content, type, toCId, quickID);
    }

    @Override
    public <T> BaseSendMsg RoomRecord(List<T> records) {
        return SSCJYMJ_RoomRecord.make(records);
    }

    @Override
    public <T> BaseSendMsg RoomEnd(T record, RoomEndResult<?> sRoomEndResult) {
        return SSCJYMJ_RoomEnd.make(this.getMJRoomRecordInfo(), this.getRoomEndResult());
    }

    @Override
    public BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum) {
        return SSCJYMJ_ChangePlayerNum.make(roomID, createPos, endSec, playerNum);
    }

    @Override
    public BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange) {
        return SSCJYMJ_ChangePlayerNumAgree.make(roomID, pos, agreeChange);
    }

    @Override
    public BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType) {
        return SSCJYMJ_ChangeRoomNum.make(roomID, roomKey, createType);
    }

    /**
     * 房间内每个位置信息 管理器
     */
    @Override
    public AbsRoomPosMgr initRoomPosMgr() {
        return new SCJYMJRoomPosMgr(this);
    }

    @Override
    public GetRoomInfo getRoomInfo(long pid) {
        S_GetRoomInfo ret = new S_GetRoomInfo();
        // 设置房间公共信息
        this.getBaseRoomInfo(ret);
        if (null != this.getCurSet()) {
            ret.setSet(this.getCurSet().getNotify_set(pid));
        } else {
            ret.setSet(new SCJYMJRoomSetInfo());
        }
        return ret;
    }

    /**
     * 清除记录。
     */
    @Override
    public void clearEndRoom() {
        super.clear();
        cfg = null;// 开房配置
        kexuanwanfa = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getCfg() {
        return (T) this.cfg;
    }

    /**
     * 操作飘
     */
    public void opPiao(WebSocketRequest request, long pid, int value) {
        try {
            lock();
            if (null == getCurSet()) {
                request.error(ErrorCode.NotAllow, "opPiao null == mCurSet");
                return;
            }
            AbsRoomPos pos = this.getRoomPosMgr().getPosByPid(pid);
            if (null == pos) {
                request.error(ErrorCode.NotAllow, "not find your pos:");
                return;
            }
            SCJYMJPiao maiZi = SCJYMJPiao.valueOf(value);
            if (SCJYMJPiao.Error.equals(maiZi)) {
                request.error(ErrorCode.NotAllow, "opPiao error value");
                return;
            }
            ((SCJYMJRoomSet) getCurSet()).opPiao(request, pos.getPosID(), maiZi);
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        } finally {
            unlock();
        }
    }

    /**
     * 是否需要解散次数
     *
     * @return
     */
    @Override
    public boolean needDissolveCount() {
        return true;
    }

    /**
     * 获取解散次数
     *
     * @return
     */
    @Override
    public int getJieShanShu() {
        return SCJYMJJieSanShu.valueOf(this.getRoomCfg().jiesancishu).value;
    }

    /**
     * 存在有玩家离开、踢出清空所有玩家准备状态
     *
     * @return T: 清空,F:不清空
     */
    public boolean existLeaveClearAllPosReady() {
        return true;
    }

    /**
     * 30秒未准备自动退出
     *
     * @return
     */
    @Override
    public boolean is30SencondTimeOut() {
        return checkGaoJiXuanXiang(GaoJiTypeEnum.SECOND_TIMEOUT_30);
    }


}
