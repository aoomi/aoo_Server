package business.global.pk.xcpdk;

import business.global.room.RoomRecordMgr;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;
import business.global.room.pk.PockerRoom;
import business.xcpdk.c2s.cclass.XCPDKRoomSetInfo;
import business.xcpdk.c2s.cclass.XCPDKRoom_RecordPosInfo;
import business.xcpdk.c2s.cclass.XCPDK_define;
import business.xcpdk.c2s.cclass.XCPDK_define.XCPDK_WANFA;
import business.xcpdk.c2s.iclass.*;
import cenum.ChatType;
import cenum.ClassType;
import cenum.room.GameRoomConfigEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.PKRoom_Record;
import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.iclass.S_GetRoomInfo;
import jsproto.c2s.iclass.room.SBase_Dissolve;
import jsproto.c2s.iclass.room.SBase_PosLeave;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏房间
 * 
 * @author Administrator
 *
 */
public class XCPDKRoom extends PockerRoom {
	// 房间配置
	private CXCPDK_CreateRoom roomCfg = null;
	private XCPDKConfigMgr configMgr = new XCPDKConfigMgr();
	protected int m_lastWinPos = -1;//记录上局赢家
	public boolean isEnd = false;

	protected XCPDKRoom(BaseRoomConfigure<CXCPDK_CreateRoom> baseRoomConfigure, String roomKey, long ownerID) {
		super(baseRoomConfigure, roomKey, ownerID);
		initShareBaseCreateRoom(CXCPDK_CreateRoom.class, baseRoomConfigure);
		this.roomCfg = (CXCPDK_CreateRoom) baseRoomConfigure.getBaseCreateRoom();
	}

	public int getLastWinPos() {
		return this.m_lastWinPos>=0?this.m_lastWinPos:0;
	}

	public void setLastWinPos(int lastWinPos) {
		this.m_lastWinPos = lastWinPos;
	}

	
	/**
	 * 清除记录。
	 */
	@Override
	public void clearEndRoom() {
		super.clear();
		this.roomCfg = null;
		this.configMgr = null;
	}
	
	/**
	 * 玩法
	 */
	public boolean isWanFaByType(XCPDK_WANFA wanfa) {
		return getRoomCfg().getKexuanwanfa().contains(wanfa.value());
	}

	/**
	 * 房间内每个位置信息 管理器
	 */
	@Override
	public AbsRoomPosMgr initRoomPosMgr() {
		return new XCPDKRoomPosMgr(this);
	}

	/**
	 * 获取房间配置
	 * 
	 * @return
	 */
	public CXCPDK_CreateRoom getRoomCfg() {
		if (this.roomCfg == null) {
			initShareBaseCreateRoom(CXCPDK_CreateRoom.class, getBaseRoomConfigure());
			return (CXCPDK_CreateRoom) getBaseRoomConfigure().getBaseCreateRoom();
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

	@SuppressWarnings("rawtypes")
	@Override
	public GetRoomInfo getRoomInfo(long pid) {
		S_GetRoomInfo ret = new S_GetRoomInfo();
		// 设置房间公共信息
		this.getBaseRoomInfo(ret);
		if (null != this.getCurSet()) {
			ret.setSet(this.getCurSet().getNotify_set(pid));
		} else {
			ret.setSet(new XCPDKRoomSetInfo());
		}
		return ret;
	}

	/**
	 * 托管
	 *
	 * @param roomID      房间id
	 * @param pid         pid
	 * @param pos         pos
	 * @param trusteeship 托管
	 * @return {@link BaseSendMsg}
	 */
	@Override
	public BaseSendMsg Trusteeship(long roomID, long pid, int pos, boolean trusteeship) {
		SXCPDK_Trusteeship make = SXCPDK_Trusteeship.make(roomID, pid, pos, trusteeship);
		make.secTotal = getCurSet()!=null?((XCPDKRoomSet)getCurSet()).getTime1(pos):-1;
		return make;
	}


	@Override
	public BaseSendMsg PosLeave(SBase_PosLeave posLeave) {
		return SXCPDK_PosLeave.make(posLeave);
	}

	@Override
	public BaseSendMsg LostConnect(long roomID, long pid, boolean isLostConnect,boolean isShowLeave) {
		return SXCPDK_LostConnect.make(roomID, pid, isLostConnect,isShowLeave);
	}

	@Override
	public BaseSendMsg PosContinueGame(long roomID, int pos) {
		return SXCPDK_PosContinueGame.make(roomID, pos);
	}

	@Override
	public BaseSendMsg PosUpdate(long roomID, int pos, RoomPosInfo posInfo, int custom) {
		return SXCPDK_PosUpdate.make(roomID, pos, posInfo, custom);
	}

	@Override
	public BaseSendMsg PosReadyChg(long roomID, int pos, boolean isReady) {
		return SXCPDK_PosReadyChg.make(roomID, pos, isReady);
	}

	@Override
	public BaseSendMsg Dissolve(SBase_Dissolve dissolve) {
		return SXCPDK_Dissolve.make(dissolve);
	}

	@Override
	public BaseSendMsg StartVoteDissolve(long roomID, int createPos, int endSec) {
		return SXCPDK_StartVoteDissolve.make(roomID, createPos, endSec);
	}

	@Override
	public BaseSendMsg PosDealVote(long roomID, int pos, boolean agreeDissolve,int endSec) {
		return SXCPDK_PosDealVote.make(roomID, pos, agreeDissolve);
	}

	@Override
	public BaseSendMsg Voice(long roomID, int pos, String url) {
		return SXCPDK_Voice.make(roomID, pos, url);
	}

	@Override
	public <T> BaseSendMsg RoomRecord(List<T> records) {
		return SXCPDK_RoomRecord.make(records);
	}

	@Override
	public void setEndRoom() {
		if (null != this.getCurSet()) {
			if (getHistorySet().size() > 0) {
				// 增加房局记录
				RoomRecordMgr.getInstance().add(this);
				this.getRoomPosMgr().notify2All(SXCPDK_RoomEnd.make(this.getPKRoomRecordInfo()));
				refererReceiveList();
			}
		}
	}

	/**
	 * 构建房间回放返回给客户端
	 * @return 通知结构体
	 */
	public PKRoom_Record getPKRoomRecordInfo(){
		PKRoom_Record pkRoom_record = new PKRoom_Record();
		pkRoom_record.setCnt = this.getHistorySetSize();
		pkRoom_record.recordPosInfosList  = this.getRecordPosInfoList();
		pkRoom_record.roomID = this.getRoomID();
		pkRoom_record.endSec = this.getGameRoomBO().getEndTime();
		return pkRoom_record;
	}

	@Override
	protected List<PKRoom_RecordPosInfo> getRecordPosInfoList() {
		List<PKRoom_RecordPosInfo> sRecord = new ArrayList<PKRoom_RecordPosInfo>();
		for(int i = 0; i < this.getPlayerNum() ; i++){
			XCPDKRoomPos roomPos = (XCPDKRoomPos) this.getRoomPosMgr().getPosByPosID(i);
			PKRoom_RecordPosInfo posInfo = roomPos.initAndReturnResult(new XCPDKRoom_RecordPosInfo());
			posInfo.flatCount = roomPos.getFlat();
			posInfo.loseCount = roomPos.getLose();
			posInfo.winCount = roomPos.getWin();
			
			posInfo.point = roomPos.getPoint();
			posInfo.pos = i;
			posInfo.pid = roomPos.getPid();
			posInfo.setMaxPoint = roomPos.maxPoint;
			posInfo.outBombSize = roomPos.outBombNum;
			posInfo.setSportsPoint(roomPos.sportsPoint());
			sRecord.add(posInfo);
		}
		return sRecord;
	}

	@Override
	public void startNewSet() {
		// 更新连续托管局数
		for(int i = 0; i < getPlayerNum(); i++){
			XCPDKRoomPos XCPDKRoomPos = (XCPDKRoomPos)getRoomPosMgr().getPosByPosID(i);
			if(XCPDKRoomPos.isTrusteeship()){ // 托管
				XCPDKRoomPos.addTuoGuanSetCount();
			}
		}
		this.setCurSetID(this.getCurSetID() + 1);
		this.createSet();
		// 每个位置，清空准备状态
		this.getRoomPosMgr().clearGameReady();
		//清理开始标志
		((XCPDKRoomPosMgr)this.getRoomPosMgr()).clearBeginFlag();
	}

	//创建set
	public void  createSet(){
		if (null != this.getCurSet()) {
			this.getCurSet().clear();
			this.setCurSet(null);
		}
		this.setCurSet(new XCPDKRoomSet_FJ( this));
		this.getRoomTyepImpl().roomSetIDChange();
	}
	


	@Override
	public int getPlayingCount() {
		return this.getPlayerNum();
	}

	/**
	 * @return configMgr
	 */
	public XCPDKConfigMgr getConfigMgr() {
		return configMgr;
	}

	@Override
	public void roomTrusteeship(int pos) {
		((XCPDKRoomSet) this.getCurSet()).roomTrusteeship(pos);
	}

	@Override
	public void RobotDeal(int pos) {
		((XCPDKRoomSet) this.getCurSet()).roomTrusteeship(pos);
	}


	@Override
	public void cancelTrusteeship(AbsRoomPos pos) {
		((XCPDKRoomSet) this.getCurSet()).roomTrusteeship(pos.getPosID());
	}
	
	@Override
	public boolean isGodCard() {
		// TODO 自动生成的方法存根
		return this.getConfigMgr().isGodCard();
	}

	@Override
	public BaseSendMsg XiPai(long roomID, long pid, ClassType cType) {
		return SXCPDK_XiPai.make(roomID, pid, cType);
	}

	@Override
	public BaseSendMsg ChatMessage(long pid, String name, String content, ChatType type, long toCId, int quickID) {
		return SXCPDK_ChatMessage.make(pid, name, content, type, toCId, quickID);
	}

	@Override
	public boolean isCanChangePlayerNum() {
		return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(GameRoomConfigEnum.FangJianQieHuanRenShu.ordinal());
	}

	@Override
	public BaseSendMsg ChangePlayerNum(long roomID, int createPos, int endSec, int playerNum) {
		return SXCPDK_ChangePlayerNum.make(roomID, createPos, endSec, playerNum);
	}

	@Override
	public BaseSendMsg ChangePlayerNumAgree(long roomID, int pos, boolean agreeChange) {
		return SXCPDK_ChangePlayerNumAgree.make(roomID, pos, agreeChange);
	}

	@Override
	public BaseSendMsg ChangeRoomNum(long roomID, String roomKey, int createType) {
		return SXCPDK_ChangeRoomNum.make(roomID, roomKey, createType);
	}

	@Override
	public SData_Result playerReady(boolean isReady, long pid) {
		try {
			lock();
			AbsRoomPos roomPos = this.getRoomPosMgr().getPosByPid(pid);
			if (roomPos == null) {
				// 找不到指定的位置信息
				return SData_Result.make(ErrorCode.NotAllow, "not in pos");
			}
			if (!RoomState.Init.equals(this.getRoomState())) {
				// 房间不处于初始阶段
				return SData_Result.make(ErrorCode.NotAllow, "playerReady RoomState Init :{%s}", this.getRoomState());
			}
			roomPos.setReady(isReady);
			if(this.getRoomPosMgr().isAllReady()){
				atOnceSetStart();
			}
			return SData_Result.make(ErrorCode.Success);
		} finally {
			unlock();
		}
	}

	/**
	 * 立即开始游戏
	 */
	public void atOnceSetStart(){
		// 设置房间状态
		this.setRoomState(RoomState.Playing);
		// 创建新亲友圈房间
		getRoomTyepImpl().createNewSetRoom();
		// 游戏开始操作
		startGameBase();
	}

//	/**
//	 * 继续功能
//	 */
//	@Override
//	protected void continueRoom() {
//		ContinueRoomInfo continueRoomInfo=new ContinueRoomInfo();
//		continueRoomInfo.setRoomID(this.getRoomID());
//		continueRoomInfo.setBaseRoomConfigure(this.getBaseRoomConfigure().deepClone());
//		continueRoomInfo.setRoomEndTime(this.getGameRoomBO().getEndTime());
//		continueRoomInfo.setPlayerIDList(this.getRoomPidAll());
//		ContinueRoomInfoMgr.getInstance().putContinueRoomInfo(continueRoomInfo);
//	}

	@Override
	public int getTimerTime() {
		return 200;
	}


	/**
	 * 房主是否需要准备
	 * @return
	 */
	@Override
	public boolean ownerNeedReady(){
		return true;
	}

	@Override
	public boolean autoReadyGame() {
		return !isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_SHOUDONGZHUNBEI);
	}

	/**
	 * 存在有玩家离开、踢出清空所有玩家准备状态
	 * @return T: 清空,F:不清空
	 */
	public boolean existLeaveClearAllPosReady() {
		return true;
	}

	/**
	 * 30秒未准备自动退出
	 * @return
	 */
	@Override
	public boolean is30SencondTimeOut() {
		return getRoomCfg().getGaoji().contains(4);
	}

	/**
	 * 是否需要解散次数
	 * @return
	 */
	@Override
	public boolean needDissolveCount(){
		return true;
	}

	/**
	 * 获取解散次数
	 * @return
	 */
	@Override
	public int getJieShanShu(){
		return 3;
	}

	/**
	 * 是否小局自动解散
	 *
	 * @return boolean
	 */
	public boolean isSetAutoJieSan() {
		return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(XCPDK_define.XCPDKGameRoomConfigEnum.SetAutoJieSan.ordinal());
	}

	/**
	 * 是否小局自动解散
	 *
	 * @return boolean
	 */
	public boolean isSetAutoJieSan2() {
		return this.getBaseRoomConfigure().getBaseCreateRoom().getFangjian().contains(XCPDK_define.XCPDKGameRoomConfigEnum.SetAutoJieSan2.ordinal());
	}

	/**
	 * 是否是四带n
	 */
	public boolean isSiDaiByNum(int daiNum) {
		return getRoomCfg().sidai.contains((daiNum-1));
	}
	/**
	 * 竞技点能否低于零
	 * @returnx
	 */
	@Override
	public boolean isRulesOfCanNotBelowZero(){
		return this.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_NOTBELOWZERO);
	}

	/**
	 * 只能赢当前身上分
	 * @return
	 */
	@Override
	public boolean isOnlyWinRightNowPoint(){
		return isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_OnlyWinRightNowPoint);
	}

	@Override
	public boolean isEnd(){
		return isEnd;
	}

	/**
	 * 是否是十进制
	 * @return
	 */
	public boolean isDecimalism() {
		// TODO 自动生成的方法存根
		return this.getConfigMgr().isDecimalism();
	}

	/**
	 * 神牌消息
	 *
	 * @param msg
	 * @param pid
	 */
	@Override
	public void godCardMsg(String msg, long pid) {
		if (isGodCard() && "x".equals(msg)) {
			getCurSet().endSet();
		}
	}

	/**
	 * 是否是333的小炸弹
	 * @return
	 */
	public boolean is333Zha(){
		return getCurSet()!=null&&((XCPDKRoomSet)getCurSet()).m_FirstOpCard == 0x23;
	}

	/**
	 * 飘花
	 */
	public void opBaDaDui(WebSocketRequest request, long pid, CXCPDK_BaDaDui data) {
		try {
			lock();
			if (null == this.getCurSet()) {
				request.error(ErrorCode.NotAllow, "");
				return;
			}
			XCPDKRoomSet roomSet = (XCPDKRoomSet) this.getCurSet();
			roomSet.opBaDaDui(request, pid, data);
		} catch (Exception e) {
			CommLogD.error(e.getMessage());
		} finally {
			unlock();
		}
	}
}
