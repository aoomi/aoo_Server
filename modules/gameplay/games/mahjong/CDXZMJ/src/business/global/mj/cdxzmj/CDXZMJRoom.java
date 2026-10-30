package business.global.mj.cdxzmj;

import business.cdxzmj.c2s.cclass.CDXZMJResults;
import business.cdxzmj.c2s.iclass.*;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateSetPos;
import business.global.room.base.AbsRoomPos;
import business.global.room.mj.MahjongRoom;
import cenum.ChatType;
import cenum.ClassType;
import cenum.room.GaoJiTypeEnum;
import cenum.room.SetState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.room.SBase_Dissolve;
import jsproto.c2s.iclass.room.SBase_PosLeave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * 模板麻将游戏房间
 *
 * @author Administrator
 */
public class CDXZMJRoom extends MJTemplateRoom {
    /**
     * 房间配置
     */
    private CCDXZMJ_CreateRoom roomCfg = null;

    protected CDXZMJRoom(BaseRoomConfigure<CCDXZMJ_CreateRoom> baseRoomConfigure, String roomKey, long ownerID) {
        super(baseRoomConfigure, roomKey, ownerID);
        initShareBaseCreateRoom(CCDXZMJ_CreateRoom.class, baseRoomConfigure);
        this.roomCfg = (CCDXZMJ_CreateRoom) baseRoomConfigure.getBaseCreateRoom();
    }

    /**
     * 房间内每个位置信息 管理器
     */
    @Override
    public CDXZMJRoomMgr initRoomPosMgr() {
        return new CDXZMJRoomMgr(this);
    }

    @Override
    public MJTemplateRoomEnum.DingQue wanFa_DingQue() {
        if (getPlayerNum() >= 3) {
            return MJTemplateRoomEnum.DingQue.DING_QUE;
        }
        return MJTemplateRoomEnum.DingQue.NOT;
    }

    /**
     * 一炮多响并且庄胡
     *
     * @return
     */
    @Override
    public MJTemplateRoomEnum.YPDXLunZhuang_ZhuangHu wanFa_YPDXLunZhuang_ZhuangHu() {
        return MJTemplateRoomEnum.YPDXLunZhuang_ZhuangHu.NOT;
    }

    /**
     * 客户端显示听牌的分
     *
     * @return
     */
    @Override
    public boolean isWanFaShowTingHuPoint() {
        return true;
    }

    /**
     * 换张
     *
     * @return
     */
    @Override
    public MJTemplateRoomEnum.ChangeCardType wanFa_ChangeCardType() {
        if (RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.HUANG_SAN_ZHANG)) {
            return MJTemplateRoomEnum.ChangeCardType.SAME_COLOR;
        } else {
            return MJTemplateRoomEnum.ChangeCardType.NOT;
        }
    }

    /**
     * 换张 顺序
     *
     * @return
     */
    @Override
    public MJTemplateRoomEnum.ChangeCardOderBy wanFa_ChangeCardType_OrderBy() {
        return MJTemplateRoomEnum.ChangeCardOderBy.RANDOM_1TO1;
    }


    @Override
    public int getWanfa() {
        return this.getRoomCfg().getWanfa();
    }


    /**
     * 获取房间配置
     *
     * @return
     */
    public CCDXZMJ_CreateRoom getRoomCfg() {
        if (this.roomCfg == null) {
            initShareBaseCreateRoom(CCDXZMJ_CreateRoom.class, getBaseRoomConfigure());
            return (CCDXZMJ_CreateRoom) getBaseRoomConfigure().getBaseCreateRoom();
        }
        return this.roomCfg;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCfg() {
        return (T) getRoomCfg();
    }

    @Override
    public String dataJsonCfg() {
        // 获取房间配置				
        return new Gson().toJson(this.getRoomCfg());
    }

    @Override
    public <E> boolean RoomCfg(E m) {
        return roomCfg.getKexuanwanfa().contains(((CDXZMJRoomEnum.KeXuanWanFa) m).ordinal());
    }

    @Override
    protected AbsMJSetRoom newMJRoomSet(int curSetID, MahjongRoom room, int dPos) {
        return new CDXZMJRoomSet(curSetID, (CDXZMJRoom) room, dPos);
    }


    @Override
    public BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship) {
        return SCDXZMJ_Trusteeship.make(roomID, pid, pos, trusteeship);
    }


    @Override
    public BaseSendMsg PosLeave(SBase_PosLeave posLeave) {
        return SCDXZMJ_PosLeave.make(posLeave);
    }


    @Override
    public BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect, boolean isShowLeave) {
        return SCDXZMJ_LostConnect.make(roomID, pid, isLostConnect, isShowLeave);
    }

    @Override
    public BaseSendMsg PosContinueGame(long roomID, int pos) {
        return SCDXZMJ_PosContinueGame.make(roomID, pos);
    }

    @Override
    public BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom) {
        return SCDXZMJ_PosUpdate.make(roomID, pos, posInfo, custom);
    }

    @Override
    public BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady) {
        return SCDXZMJ_PosReadyChg.make(roomID, pos, isReady);
    }

    @Override
    public BaseSendMsg Dissolve(SBase_Dissolve dissolve) {
        return SCDXZMJ_Dissolve.make(dissolve);
    }

    @Override
    public BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec) {
        return SCDXZMJ_StartVoteDissolve.make(roomID, createPos, endSec);
    }

    @Override
    public BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve, int endSec) {
        return SCDXZMJ_PosDealVote.make(roomID, pos, agreeDissolve);
    }

    @Override
    public BaseSendMsg Voice(long roomID, int pos, String url) {
        return SCDXZMJ_Voice.make(roomID, pos, url);
    }

    @Override
    public <T> BaseSendMsg RoomRecord(List<T> records) {
        return SCDXZMJ_RoomRecord.make(records);
    }


    @Override
    public BaseSendMsg XiPai(long roomID, long pid, ClassType cType) {
        return SCDXZMJ_XiPai.make(roomID, pid, cType);
    }

    @Override
    public BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId, int quickID) {
        return SCDXZMJ_ChatMessage.make(pid, name, content, type, toCId, quickID);
    }

    @Override
    public BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum) {
        return SCDXZMJ_ChangePlayerNum.make(roomID, createPos, endSec, playerNum);
    }

    @Override
    public BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange) {
        return SCDXZMJ_ChangePlayerNumAgree.make(roomID, pos, agreeChange);
    }

    @Override
    public BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType) {
        return SCDXZMJ_ChangeRoomNum.make(roomID, roomKey, createType);
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


    /**
     * 飘分
     */
    public void opPiaoFen(WebSocketRequest request, long pid, CCDXZMJ_PiaoFen data) {
        try {
            lock();
            if (null == this.getCurSet()) {
                request.error(ErrorCode.NotAllow, "CDXZMJSet null");
                return;
            }
            CDXZMJRoomSet roomSet = (CDXZMJRoomSet) this.getCurSet();
            if (!roomSet.getState().equals(SetState.WaitingEx)) {
                request.error(ErrorCode.NotAllow, "opPiaoFen cur setstate=" + roomSet.getState());
                return;
            }
            if (data.piaoFen <= -1 || data.piaoFen > 5) {
                request.error(ErrorCode.NotAllow, "opPiaoFen value error piaoFen={} ", data.piaoFen);
                return;
            }
            AbsRoomPos posByPid = getRoomPosMgr().getPosByPid(pid);
            if (posByPid == null) {
                return;
            }
            MJTemplateSetPos setPos = (CDXZMJSetPos) roomSet.getMJSetPos(posByPid.getPosID());
            setPos.setPiaoFen(data.piaoFen);
            notifyPiaoFen(setPos, roomSet.getState());
            ((CDXZMJRoomSet) getCurSet()).doPiaoState();
            if (null != request) {
                request.response();
            }
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        } finally {
            unlock();
        }
    }

    public void notifyPiaoFen(MJTemplateSetPos setPos, SetState state) {
        getRoomPosMgr().notify2All(SCDXZMJ_PiaoFen.make(getRoomID(), setPos.getPosID(), setPos.getPiaoFen()));
    }


    public void setRoomCfg(CCDXZMJ_CreateRoom roomCfg) {
        this.roomCfg = roomCfg;
    }

    /**
     * 是否需要解散次数
     *
     * @return
     */
    @Override
    public boolean needDissolveCount() {
        return getRoomCfg().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.JieSanCishu5.ordinal()) || getRoomCfg().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.JieSanCishu3.ordinal());
    }

    /**
     * 获取解散次数
     *
     * @return
     */
    @Override
    public int getJieShanShu() {
        if (getRoomCfg().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.JieSanCishu3.ordinal())) {
            return 3;
        }
        if (getRoomCfg().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.JieSanCishu5.ordinal())) {
            return 5;
        }
        return 3;
    }

    @Override
    public double initMinPoint() {
        return 0;
    }

    /**
     * 自动准备游戏 玩家加入房间时，自动进行准备。
     */
    @Override
    public boolean autoReadyGame() {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian()
                .contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.ZiDongZhunBei.ordinal());
    }

    @Override
    public <T> BaseSendMsg RoomEnd(T record, RoomEndResult<?> sRoomEndResult) {
        RoomEndResult roomEndResult = this.getRoomEndResult();
        roomEndResult.getResultsList().stream().max(Comparator.comparingInt(CDXZMJResults::getPoint)).ifPresent(n -> ((CDXZMJResults) n).setWinner(true));
        roomEndResult.getResultsList().stream().max(Comparator.comparingInt(CDXZMJResults::getDianPaoPoint)).ifPresent(n -> ((CDXZMJResults) n).setDianPaoWang(true));

        return SCDXZMJ_RoomEnd.make(this.getMJRoomRecordInfo(), roomEndResult);
    }


    /**
     * 是否能切换人数
     *
     * @return boolean
     */
    @Override
    public boolean isCanChangePlayerNum() {
        return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(CDXZMJRoomEnum.CDXZMJGameRoomConfigEnum.FangJianQieHuanRenShu.ordinal());
    }

    @Override
    public boolean isWanFa_TianTing() {
        return true;
    }

    /**
     * 飘分
     */
    public void opAutoChoose(WebSocketRequest request, long pid, CCDXZMJ_AutoChoose data) {
        try {
            lock();
            if (null == this.getCurSet()) {
                request.error(ErrorCode.NotAllow, "CDXZMJSet null");
                return;
            }
            CDXZMJRoomSet roomSet = (CDXZMJRoomSet) this.getCurSet();
            AbsRoomPos posByPid = getRoomPosMgr().getPosByPid(pid);
            if (posByPid == null) {
                return;
            }
            CDXZMJSetPos setPos = (CDXZMJSetPos) roomSet.getMJSetPos(posByPid.getPosID());
            if (data.autoHu >= 0) {
                setPos.setAutoHu(data.autoHu);
            }
            if (data.autoOut >= 0) {
                setPos.setAutoOut(data.autoOut);
            }
            this.getRoomPosMgr().notify2Pos(setPos.getPosID(), SCDXZMJ_AutoChoose.make(this.getRoomID(), setPos.getAutoHu(), setPos.getAutoOut()));
        } catch (Exception e) {
            CommLogD.error(e.getMessage());
        } finally {
            unlock();
        }
    }

    /**
     * @return
     */
    @Override
    public List<MJTemplateRoomEnum.AutoOpType> wanFa_AutoTypeList() {
        return new ArrayList<>(Arrays.asList(MJTemplateRoomEnum.AutoOpType.Out, MJTemplateRoomEnum.AutoOpType.Hu));
    }
}			
