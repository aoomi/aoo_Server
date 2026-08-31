package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Cfg;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Set;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_KongPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_MoShi;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_XuanZhuang;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_Zhuang;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_ZhuangSet;
import jsproto.c2s.iclass.S1102_EnterRoom;
import jsproto.c2s.iclass.SPlayer_XiPai;
import jsproto.c2s.iclass.zypk.CZYPK_Zhuang;
import jsproto.c2s.iclass.zypk.SZYPK_GetRoomInfo;
import business.global.entity.GameJson;
import business.global.room.delegate.PockerRoom;
import business.global.room.delegate.RoomPosDelegateAbstract;
import cenum.ShareDefine;
import cenum.CEnum.ClassType;
import cenum.CEnum.RoomState;

import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;


public class ZYPKRoom<T> extends PockerRoom<T> {
	public ZYPKRoom_Cfg cfg; //创建配置
	public List<ZYPKRoomSet > historySet = new ArrayList<>(); // 历史局
	public ZYPKRoomSet curSet = null; // 当前局
	private ArrayList<Long>  m_XiPaiPidList = new ArrayList<Long>(); //洗牌pid 大于0标识洗牌玩家pid
	private ZYPKConfigMgr configMgr = new ZYPKConfigMgr();
	private ZYPK_Zhuang zhuangZYPK = ZYPK_Zhuang.Luan;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void createInit(long ownerID, String key) {
		
		// TODO 自动生成的方法存根
		this.cfg = (ZYPKRoom_Cfg) this.bRoomConfigure.getRoomCfg();
		this.m_createSec = CommTime.nowSecond();
		
		
		this.m_posMgr = new ZYPKRoomPosMgr(this);		
//		if (this.getRoomConfigure().getCreateType() == CreateType.NORMAL.value()) {
//			this.m_posMgr.getPos(0).setReady(true);
//		}

		
		
		//将配置转成JSON
		GameJson<ZYPKRoom_Cfg> gameJson = new GameJson<ZYPKRoom_Cfg>(this.cfg);
		String dataJsonCfg = gameJson.toJson(ZYPKRoom_Cfg.class);
			
		
		m_GameRoomBO.setCreateTime(this.m_createSec);
		m_GameRoomBO.setOwnner(getOwnerID());
		m_GameRoomBO.setSetCount(this.getCount());
		m_GameRoomBO.setPlayerNum(this.getPlayerNum());
		m_GameRoomBO.setDataJsonCfg(dataJsonCfg);
		m_GameRoomBO.setGameType(getGameType().value());
		m_GameRoomBO.setRoomType(this.bRoomConfigure.getPrizeType().value());
		m_GameRoomBO.setRoomKey(key);
		m_GameRoomBO.setType(ClassType.PK.value());
		m_GameRoomBO.insert_sync();		
	}

	@Override
	public int getHistorySetSize() {
		return historySet.size();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getCurSet() {
		return (T) this.curSet;
	}

	@Override
	public boolean getCurSetUpdate(int sec) {
		return curSet.update(sec);
	}

	@Override
	public void startNewSet() {
		this.curSetID++;
		this.curSet = this.createSet();
		// 每个位置，清空准备状态
		getPosMgr().clearReady();
	}
	
	//创建set
	public ZYPKRoomSet createSet(){
		ZYPKRoomSet set = new ZYPKRoomSet( this);
		return set;
	}

	@Override
	public void addHistorySet() {
		if (!historySet.contains(curSet)) {
			historySet.add(curSet);
			m_GameRoomBO.saveEndTime(CommTime.nowSecond());
		}
	}

	@Override
	public void cancelTrusteeship(RoomPosDelegateAbstract<?> pos) {
		
	}

	@Override
	public void roomTrusteeship(int pos) {
	}

	@Override
	public void setEndRoom() {
		
		// 房间管理注销
		if (historySet.size() > 0) {

		} else {
			m_GameRoomBO.del();
		}		
	}
	

	@Override
	public void calcEnd() {
	
		
		if (null != this.m_GameRoomBO) {
//			SPDK_RoomEndResult sEndResult =  getRoomEndResult();
//			if (null != sEndResult) {
//				String gsonEndResult = new Gson().toJson(sEndResult);
//				this.m_GameRoomBO.setDataJsonRes(gsonEndResult);
//			}
		}
		String playerPosList = new Gson().toJson(getPosMgr().getRoomPlayerPosList());
		int num = ((ZYPKRoomPosMgr<?>)this.getPosMgr()).getPlayTheGameNum();
		this.m_GameRoomBO.setPlayerNum(num);
		this.m_GameRoomBO.setPlayerList(playerPosList);
		this.m_GameRoomBO.setEndTime(CommTime.nowSecond());
		this.m_GameRoomBO.saveAll();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T getAllDisplayInfo(long pid) {
		SZYPK_GetRoomInfo ret = new SZYPK_GetRoomInfo();
		this.getBaseRoomInfo(ret);
		ret.cfg = this.cfg; // 开房配置
		
		if (null != this.curSet) {
			ret.set = this.curSet.getNotify_set(pid);
		} else {
			ret.set = new ZYPKRoom_Set<Object>(); // 当前局的信息
		}
		
		
		return (T) ret;
	}

	@SuppressWarnings({"unused" })
	@Override
	public T getRecord() {
//		List<PDKRoom_SetEnd> records = new ArrayList<>();
		for (ZYPKRoomSet set : historySet)
		{
//			records.add(set.getNotify_setEnd());
		}
		return null;
	}



	@Override
	public void setCurSet(T curSet) {
		if (curSet == null) {
			this.curSet = null;
		}else {
			this.curSet = (ZYPKRoomSet) curSet;	
		}
	}

	@Override
	public S1102_EnterRoom getEnterRoomInfo() {
		// TODO 自动生成的方法存根
		return S1102_EnterRoom.make(this.getRoomID(), 0, this.getGameType(), 0);
	}
	
	//解散房间
	@Override
	public  boolean isAllAgree(){
		return this.m_posMgr.isAllAgreeEx(this.getPlayerNum());
	}
	
	/*
	 * 加入房间的其他条件 条件不满足不进入
	 * */
	@Override
	public boolean enterRoomOtherCondition(long pid){
		if(this.cfg.isKeptOutAfterStartGame && this.getState() != RoomState.Init) {
			return false;
		}
		return true;
	}
	
	
	/*
	 * 主动离开房间的其他条件 条件不满足不退出
	 * */
	@Override
	public boolean exitRoomOtherCondition(WebSocketRequest request, long pid, int posIndex){
//		if(PrizeType.Gold == this.getPrizeType())  return true;
		// 玩家玩过游戏就不能离开
		ZYPKRoomPos<?> pos = (ZYPKRoomPos<?>) this.m_posMgr.getPos(posIndex);
		if (pos != null && pos.isPlayTheGame()) {
			request.error(ErrorCode.ExitROOM_ERROR, "pos.isPlayTheGame():" + pos.isPlayTheGame());
			return false;
		}
		return true;
	}
	
	/*
	 * 开始游戏的其他条件 条件不满足不进入
	 * */
	public boolean startGameOtherCondition(WebSocketRequest request, long pid) {
		int count = 0;
		boolean isQ = false;
		for(RoomPosDelegateAbstract<?> pos : this.getPosMgr().posList){
			if (pos.posID == this.getDPos()) {
				isQ = true;
			}
			if(pos.isReady){
				count++;
			}
		}
		
		if (!isQ) {
			request.error(ErrorCode.NotFind_Pos, "ready player is last two!");
			return false;
		}
		if(count < 2){
			request.error(ErrorCode.NotAllow, "ready player is last two!");
			return false;
		}
		return true;
	}

	
	
	//当前几个人在玩
	public int getPlayingCount(){
		int count = 0;
		if (this.curSet != null) {
			count = this.curSet.getPlayingCount();
		} else {
			count = this.getPlayerNum();
		}
		return count;
	}


	


	@Override
	public boolean onXiPai(long pid) {
		// TODO 自动生成的方法存根
		if(!m_XiPaiPidList.contains(pid)){
			m_XiPaiPidList.add(pid);
		}
		notify2All(SPlayer_XiPai.make(getRoomID(), pid, ClassType.PK));
		return true;
	}
	
	/**
	 * @param m_isXiPai 要设置的 m_isXiPai
	 */
	public boolean isXiPaiPid() {
		return m_XiPaiPidList.size() > 0;
	}

	/**
	 * @return m_XiPaiPidList
	 */
	public ArrayList<Long> geXiPaiPidList() {
		return m_XiPaiPidList;
	}
	
	/**
	 * @return m_XiPaiPidList
	 */
	public void clearXiPaiPidList() {
		m_XiPaiPidList.clear();;
	}

	/**
	 * @return configMgr
	 */
	public ZYPKConfigMgr getConfigMgr() {
		return configMgr;
	}
	
	/*
	 * 玩法
	 * */
	public boolean isWanFaByType(ZYPK_AnNiu wanfa){
		return this.cfg.kexuanwanfa.contains(wanfa.value());
	}
	

	
	/**
	 * 获取房间人数
	 */
	@Override
	public int getPlayerNum() {
		return this.cfg.playerNum;
	}
	
	@Override
	public int getTimerTime() {
		return 500;
	}
	
	
	/**
	 * 是否选择该按钮
	 * @param anNiu
	 * @return
	 */
	public boolean isAnNiu (ZYPK_AnNiu anNiu) {
		return this.cfg.anNius.contains(anNiu.value());
	}
		
	/**
	 * 是否设置庄家
	 * @return
	 */
	public boolean isZhunag () {
		return this.cfg.zhuagnJia == ZYPK_ZhuangSet.Zhuang.value();
	}
	
	/**
	 * 选庄
	 * @return
	 */
	public ZYPK_XuanZhuang xuanZhunag () {
		return ZYPK_XuanZhuang.valueOf(this.cfg.xuanZhuang);
	}

	/**
	 * 是否庄家控牌
	 * @return
	 */
	public boolean isZhuangkongPai () {
		return this.cfg.kongPai == ZYPK_KongPai.ZhuangJia.value();
	}

	/**
	 * 是否轮流操作
	 * @return
	 */
	public boolean isLunLiu () {
		return this.cfg.moShi == ZYPK_MoShi.Luan.value();
	}
	
	/**
	 * 获取理牌数
	 * @return
	 */
	public int getLiPaiNum() {
		return this.cfg.liPai;
	}
	
	/**
	 * 获取留牌数
	 * @return
	 */
	public int getLiuPaiNum() {
		return this.cfg.liuPai;
	}
	
	/**
	 * 获取筹码数
	 * @return
	 */
	public int getChouMaNum() {
		return this.cfg.chouMa;
	}
	
	/**
	 * 获取除牌列表
	 * @return
	 */
	public ArrayList<Integer> getChuPais() {
		return this.cfg.chuPais;
	}
	
	
	
	public ZYPK_Zhuang getZhuangZYPK() {
		return zhuangZYPK;
	}

	/**
	 * 庄家设置
	 * @param request
	 * @param onZhuang
	 */
	public void onZhuang(WebSocketRequest request, CZYPK_Zhuang onZhuang) {
		// 是否庄家模式,如果不是庄家模式不能设置庄家
		if (!this.isZhunag()) {
			request.error(ErrorCode.NotAllow, "!this.room.isZhunag() not null");
			return;
		}
		// 检查设置庄家的参数是否存在
		if (null == onZhuang) {
			request.error(ErrorCode.NotAllow, "CZYPK_Zhuang not null");
			return; 			
		}
		// 获取相应的类型
		this.zhuangZYPK  = ZYPK_Zhuang.valueOf(onZhuang.opType);
		if (ZYPK_Zhuang.GuDing.equals(this.zhuangZYPK)) {
			//固定坐庄
			this.setDPos(onZhuang.targetPos);
		} else if (ZYPK_Zhuang.Luan.equals(this.zhuangZYPK)) {
			//轮流坐庄，从 -房间控制者- 开始往下轮流。
			RoomPosDelegateAbstract<?>  roomPos = this.m_posMgr.getPosByPid(this.getControllerID());
			if (null != roomPos) {
				this.setDPos(roomPos.posID);
			}	
		}
		request.response();
	}

	/**
	 * 控制者位置
	 * @return
	 */
	public int getControlPos ( ) {
		RoomPosDelegateAbstract<?>  roomPos = this.m_posMgr.getPosByPid(this.getControllerID());
		if (null != roomPos) {
			return roomPos.posID;
		}
		return 0;
	}
	
	@Override
	public void cleanEndRoom() {
		// 清除历史牌局
		this.historySet.clear();
		// 清除当前牌局
		this.curSet = null;
	}

	@Override
	protected List<PKRoom_RecordPosInfo> getRecordPosInfoList() {
		// TODO 自动生成的方法存根
		return null;
	}

	@Override
	public int getMaxPlayerNum() {
		// TODO 自动生成的方法存根
		return ShareDefine.MAXPLAYERNUM_ZYPK;
	}

	@Override
	public boolean isCanChangePlayerNum() {
		// TODO 自动生成的方法存根
		return false;
	}

}
