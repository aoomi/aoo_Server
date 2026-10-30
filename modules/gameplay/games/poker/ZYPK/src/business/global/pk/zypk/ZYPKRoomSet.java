package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jsproto.c2s.cclass.zypk.ZYPKRoomSet_Pos;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Set;
import jsproto.c2s.cclass.zypk.ZYPKSet_Pos;
import jsproto.c2s.cclass.zypk.ZYPK_Backups;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KanPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_KongPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_KongPai;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_QiangZhuang;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_XuanZhuang;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_Zhuang;
import jsproto.c2s.iclass.zypk.CZYPK_Give;
import jsproto.c2s.iclass.zypk.CZYPK_KongPai;
import jsproto.c2s.iclass.zypk.CZYPK_PlayerOp;
import jsproto.c2s.iclass.zypk.CZYPK_QiangZhuang;
import jsproto.c2s.iclass.zypk.SZYPK_Clean;
import jsproto.c2s.iclass.zypk.SZYPK_Give;
import jsproto.c2s.iclass.zypk.SZYPK_KongPai;
import jsproto.c2s.iclass.zypk.SZYPK_SetStart;
import jsproto.c2s.iclass.zypk.SZYPK_XuanZhuang;
import jsproto.c2s.iclass.zypk.SZYPK_Zhuang;
import business.global.club.ClubMgr;
import business.global.room.base.AbsRoomPos;
import cenum.room.SetState;
import cenum.room.TrusteeshipState;

import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;

import core.db.entity.clarkGame.GameSetBO;

/**
 * 自由扑克一局游戏逻辑
 * 
 * @author huaxing
 *
 */

public  class ZYPKRoomSet {
	/**房间*/
	private ZYPKRoom<?> room = null;
	/**开始游戏时间*/
	private long startMS = 0;
	/**游戏状态*/
	protected SetState state = SetState.Init;
	/**牌管理*/
	private ZYPKSetCard setCard = null;
	/**数据库*/
	public GameSetBO bo = null;
	/**庄家位置*/
	protected int dPos = 0;	//庄家位置
	/**控牌者*/
	private int kongPaiPos = -1;//控牌位置
	/**回合初始化*/
	private ZYPKSetSound curRound = null;
	/**回合记录*/
	private List<ZYPKSetSound> historyRound = new ArrayList<>();
	/**延迟发牌时间*/
	protected int InitTime = 4000;// 延迟发牌的时间
	/**线程管理器*/
	private TimerTask timerTask = null;
	/**线程管理*/
	private Timer timer = null;
	/**抢庄列表*/
	private List<Integer> qiangZhuangList = new ArrayList<Integer>();
	/**抢庄map*/
	private HashMap<Integer, Integer> qiangZhuangMap = new HashMap<Integer, Integer>();
	/**选庄状态*/
	private ZYPK_XuanZhuang xZhuang = ZYPK_XuanZhuang.Not;
	/**公共牌*/
	private ArrayList<Integer> publicCardList = new ArrayList<Integer>();
	/**留牌*/
	private ArrayList<Integer> liuPaiList = new ArrayList<Integer>();
	/**玩家列表*/
	private List<Boolean> playerList = new ArrayList<Boolean>();
	/**玩家人数*/
	private int playerNum = 0;
	/**押注筹码*/
	private int yaChouMa = 0;
	/**看牌*/
	private Op_KanPai oPpai = Op_KanPai.Kan;
	/**备份玩家的上次操作*/
	private HashMap<Long, ZYPK_Backups> backupsMap = new HashMap<Long, ZYPK_Backups>();
	/**轮庄时候使用*/
	private int nextDpos = 0;
	@SuppressWarnings("rawtypes")
	public ZYPKRoomSet(ZYPKRoom room) {
		this.room = room;
		this.xuanZhuang();
		
		ClubMgr.getInstance().roomSetIDChange(room.getClubID(), room.getRoomID(), room.getRoomKey(), room.getCurSetID());
	}
	
	

	public ZYPKRoom<?> getRoom() {
		return room;
	}



	public ZYPKSetSound getHistoryRound(int roundId) {
		return historyRound.get(roundId);
	}



	//当前几个人在玩
	public int getPlayingCount(){
		int count = 0;
		for(boolean flag : this.playerList){
			if(flag) count++;
		}
		return count;
	}

	
	// 每200ms更新1次 秒
	public boolean update(int sec) {
		boolean isClose = false;
		if (this.state == SetState.Init) {
			if (CommTime.nowMS() > this.startMS + this.InitTime) {
				this.state = SetState.Playing;
				if (!this.startNewRound()) {
//					this.endSet();
				}
			}
		} else if (this.state == SetState.Playing) {
			if (this.room.isLunLiu()) {
//				boolean isRoundClosed = this.curRound.update(sec);
//				if (isRoundClosed) {
//					if (curRound.isSetHuEnd) {
////						this.endSet();
//					}
//				}
			} else {
			}
		} else if (this.state == SetState.End) {
			isClose = true;
			
		} else if (this.state == SetState.Init) {
			
		}
		return isClose;
	}
	
	
	/**
	 * 选庄
	 */
	public void xuanZhuang () {
		// 初始化整个游戏
		this.initSetCard();
		//发牌前-确认庄家
		if (ZYPK_XuanZhuang.FaPai_Q.equals(this.room.xuanZhunag())) {
			//发牌前 定庄家
			if (!this.startTimer()) this.startKongPai();
		} else {
			//先发牌
			this.startKongPai();	
		}
	}

	
	/**
	 * 设置玩家状态并且洗底牌
	 */
	private void initSetCard() {
		//设置参与游戏的玩家
		setClean();
		initPlayer();
		for(AbsRoomPos pos : this.room.getPosMgr().posList){
			ZYPKRoomPos roomPos = (ZYPKRoomPos) pos;
			if((pos.isReady() && this.room.getCurSetID() == 1) || (this.room.getCurSetID() > 1 && pos.getPid() != 0)){
				//检查玩家状态是否已经玩过
				if (!roomPos.isPlayTheGame()) {
					//是否玩过游戏
					roomPos.setPlayTheGame(true);
					//初始化筹码
					roomPos.setChouMa(this.room.getChouMaNum());
					this.playerList.set(roomPos.getPosID(), true);
				}
				roomPos.cleanRoomPos();
			}
		}
		this.playerNum = this.playerList.size();
		// 洗底牌
		this.setCard = new ZYPKSetCard(this.room);
		//初始化扑克牌
		this.setCard.init();
		
	}

	
	
		
	// 开启新的回合
	public boolean startNewRound() {	
		if (this.curRound != null) {
			this.historyRound.add(this.curRound);
		}
		this.curRound = new ZYPKSetSound(this,this.historyRound.size() + 1); // 开启第一轮
		return this.curRound.tryStartRound();
	}
	
	/**
	 * 开始控牌设置
	 */
	public void startKongPai() {
		//检查 当前一局的选庄状态  是否发牌后确认庄家
		if (ZYPK_XuanZhuang.FaPai_H.equals(this.xZhuang)) {
			//确认所有人,庄家是谁。
			this.room.notify2All(SZYPK_XuanZhuang.make(this.room.getRoomID(), this.dPos));
			this.state = SetState.Init;
			this.startMS = CommTime.nowMS();
			return;
		}
		
		
		// 获取当前选择的控牌状态
		ZYPK_KongPai kongPaiZYPK = ZYPK_KongPai.valueOf(this.room.cfg.kongPai);
		// 通知 所有人 庄家控牌还是房主控牌
		if (this.room.isZhuangkongPai()) {
			this.kongPaiPos = this.dPos;
		} else {
			this.kongPaiPos = this.room.getControlPos();
		}
		this.room.notify2All(SZYPK_KongPai.make(this.room.getRoomID(),kongPaiZYPK, this.kongPaiPos));
	}
	
	/**
	 * 清除状态
	 */
	private void setClean () {
		this.curRound = null;
		this.timerTask = null;
		this.timer = null;
		this.bo = null;
		this.historyRound.clear();
		//清除人数
		this.playerNum = 0;
		//清除玩家列表
		this.playerList.clear();
		this.qiangZhuangList.clear();
		this.qiangZhuangMap.clear();
		this.publicCardList.clear();
		this.liuPaiList.clear();
		this.xZhuang = ZYPK_XuanZhuang.Not;
		this.kongPaiPos = -1;
		this.state = SetState.Init;
		this.yaChouMa = 0;
		this.nextDpos = 0;
		//按钮选择看牌
		if (this.room.isAnNiu(ZYPK_AnNiu.KanPai)) {
			//都不看
			this.oPpai = Op_KanPai.Not_Kan;
		} else {
			//正常
			this.oPpai = Op_KanPai.Kan;
		}
	}

	/**
	 * 获取玩家列表
	 * @return
	 */
	public List<Boolean> getPlayerList() {
		return playerList;
	}


	/**
	 * 初始玩家信息
	 * @param roomPos
	 */
	private void initPlayer() {
		for (int i = 0 ;i<this.room.getPlayerNum();i++) {
			//初始化玩家位置
			this.playerList.add(false);
		}
	}
	
	/**
	 * 检查是否有该玩家位置
	 * @param pos
	 * @return
	 */
	private boolean checkPos(int pos) {
		for (int i = 0 ;i<this.room.getPlayerNum();i++) {
			if (!this.playerList.get(i)) continue;
			if (i == pos) {
				return true;
			}
		}
		return false;
	}

	
	

	
	//设置所有玩家都准备进行下一场游戏
	public void setAllGameReady(boolean flag) {
		if(this.room.getCurSetID() >= this.room.cfg.setCount) return;
		for (RoomPosDelegateAbstract<?> pos : this.room.getPosMgr().posList) {
			if (pos.pid != 0) {
				pos.setGameReady(flag);
				if(flag) pos.clearLatelyOutCardTime();
			}
		}
	}
	
	

	/**
	 * 获取通知设置
	 * @param pid 用户ID
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ZYPKRoom_Set getNotify_set(long pid) {
		ZYPKRoom_Set ret = new ZYPKRoom_Set();
		ret.roomID = this.room.getRoomID();
		ret.setID = this.room.getCurSetID();
		ret.startTime = this.startMS;
//		ret.state = this.state;
		ret.state = this.state == SetState.Waiting ? SetState.Init:this.state;// 牌局状态
		ret.kongPaiPos = this.kongPaiPos;
		if (this.room.isZhunag()) {
			ret.dPos = this.dPos;
		} else {
			ret.dPos = -1;
		}
		ret.publicCardList = this.publicCardList;
		ret.pkCardSize = this.setCard.getPKCardSize();
		ret.liuPaiNum = this.room.getLiuPaiNum();
		ret.liuPaiList = this.liuPaiList;
		// 每个玩家的牌面
		ret.posInfo = new ArrayList<>();
		for (int i = 0; i < this.room.getPlayerNum(); i++) {
			if (!this.playerList.get(i)) continue;
			ZYPKRoomPos<?> roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(i);
			ZYPKSet_Pos zSetPos = NotifyCard(roomPos,pid);		
			ZYPKRoomSet_Pos roomSet_Pos = new ZYPKRoomSet_Pos();
			roomSet_Pos.setPid(roomPos.pid);
			roomSet_Pos.setPosID(roomPos.posID);
			roomSet_Pos.setPoint(roomPos.point);
			roomSet_Pos.setoPpai(roomPos.getoPpai());
			roomSet_Pos.setQiPai(roomPos.getQiPai());
			roomSet_Pos.setBeiShu(roomPos.getBeiShu());
			roomSet_Pos.setYaZhu(roomPos.getYaZhu());
			roomSet_Pos.setPrivateCards(zSetPos.privateCards);
			roomSet_Pos.setOutCards(zSetPos.outCards);
			ret.posInfo.add(roomSet_Pos);
		}
		
		if (this.room.isLunLiu()) {
			ZYPKRoomPos<?> roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPosByPid(pid);
			if (null != roomPos && roomPos.isPlayTheGame()) {
				if (this.state == SetState.Playing && null != curRound) {
					ret.setRound = curRound.getNotify_RoundInfo(roomPos.posID);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * 玩家牌状态
	 * @param roomPos
	 * @param pid
	 * @return
	 */
	private ZYPKSet_Pos NotifyCard(ZYPKRoomPos<?> roomPos,long pid) {
		ZYPKSet_Pos zSetPos = null;
		switch (this.oPpai) {
		case Kan:
			//看牌
			roomPos.setoPpai(this.oPpai);
			break;
		case Not_Kan:
			//不看牌
			roomPos.setoPpai(this.oPpai);
			break;
		case Ming:
			//明牌
			roomPos.setoPpai(this.oPpai);
			break;
		default:
			break;
		}
		zSetPos = roomPos.getNotifyCard(pid);
		return zSetPos;
	}
	
	
	/**
	 * 抢庄
	 */
	public void onQiangZhuang (WebSocketRequest request, CZYPK_QiangZhuang onQiangZhuang) {
		if (null == onQiangZhuang) {
			request.error(ErrorCode.NotAllow, "CZYPK_QiangZhuang not null");
			return; 
		}
		//onQiangZhuang.opType == 1 抢庄
		if (!this.qiangZhuangList.contains(onQiangZhuang.pos) &&  onQiangZhuang.opType == 1) {
			//返回通知
			request.response(SZYPK_Zhuang.make(this.room.getRoomID(),ZYPK_QiangZhuang.Qiang));
			//----------------
			this.qiangZhuangList.add(onQiangZhuang.pos);
		} else {
			request.response();	
		}
		this.qiangZhuangMap.put(onQiangZhuang.pos, onQiangZhuang.opType);
		if (this.qiangZhuangMap.size() >= this.playerNum) {
			randomDPos();
		}		
	}

	
	/**
	 * 开启定时器
	 */
	private boolean startTimer() {
		//是否庄家模式
		if (!this.room.isZhunag()) return false;
		
		if (ZYPK_Zhuang.SuiJi.equals(this.room.getZhuangZYPK())) {
			//随机坐庄
			this.dPos = this.setCard.randomSaizi(this.room.getPlayerNum());
			this.dPos = this.nextOpPos(this.dPos,false);
			return false;
		} else if (ZYPK_Zhuang.Luan.equals(this.room.getZhuangZYPK())) {
			//轮流坐庄
			if(this.nextDpos <= 0) {
				this.dPos = -1;
				this.nextDpos++;
			}
			this.dPos = this.nextOpPos(this.dPos,false);
			return false;
		} else if (ZYPK_Zhuang.GuDing.equals(this.room.getZhuangZYPK())) {
			this.dPos = this.room.getDPos();
			return false;
		}
		//-----------------------------------------------------
		this.room.notify2All(SZYPK_Zhuang.make(this.room.getRoomID(),ZYPK_QiangZhuang.Strat_Qiang));
		//-----------------------------------------------------
		//开启线程
		this.timer = new Timer();
		this.timerTask = new TimerTask() {
			@Override
			public void run() {
				randomDPos();
				cancel();
				
			}
		};
		this.timer.schedule(this.timerTask, 15 * 1000);
		return true;
	}
	
	/**
	 * 关闭定时器
	 */
	private void cancelTimer() {
		// 结束当前任务
		if (this.timerTask != null) {
			this.timerTask.cancel();
			this.timerTask = null;
		}
		// 结束定时器
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}
	}
	
	/**
	 * 通过抢庄人列表随机获取一个。
	 */
	private void randomDPos () {
		this.dPos = this.setCard.randomDPos(this.qiangZhuangList, this.playerNum);
		this.dPos = this.nextOpPos(this.dPos,false);
		this.startMS = CommTime.nowMS();
		startKongPai();
		cancelTimer();
	}

	
	

	/**
	 * 控牌操作
	 * @param request
	 * @param onKongPai
	 */
	public void onKongPai(WebSocketRequest request, CZYPK_KongPai onKongPai) {
		if (null == onKongPai) {
			request.error(ErrorCode.NotAllow, "onKongPai not null");
			return; 
		}
		
		if (!checkPos(onKongPai.pos)) {
			request.error(ErrorCode.NotAllow, "onKongPai !checkPos(onKongPai.pos)");
			return; 
		}
		
		if (this.kongPaiPos != onKongPai.pos) {
			request.error(ErrorCode.NotAllow, "kongPaiPos != onKongPai.pos");
			return;
		}
		
		
		switch (Op_KongPai.valueOf(onKongPai.opType)) {
		case FaPai:
			//发牌
			faPai(onKongPai.privateCount,onKongPai.publicCount);
			break;
		case XiPai:
			//洗牌
			xiPai();
			break;
		case ShouPai:
			//收牌
			shouPai();
			break;
		default:
			break;
		}
		request.response();
	}
	
	/**
	 * 发牌
	 */
	private void faPai(int privateCardNum,int publicCardNum) {
		//初始化玩家身上的手牌
		for (int j = 0; j < this.room.getPlayerNum(); j++) {
			if (!this.playerList.get(j)) continue;
			ZYPKRoomPos<?> roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos((this.dPos + j)% this.playerNum);
			if (null == roomPos)
				continue;
			//初始玩家身上的牌
			roomPos.init(this.setCard.popList(privateCardNum)); 
		}
		//初始公共牌
		this.publicCardList = this.setCard.popList(publicCardNum);
		
		//开始发牌
		for (int i = 0; i < this.playerNum; i++) {
			if (!this.playerList.get(i)) continue;
			long pid = this.room.getPosMgr().getPos(i).pid;
			this.room.notify2Pos(i, SZYPK_SetStart.make(this.room.getRoomID(), this.getNotify_set(pid)));
		}
		this.room.getPosMgr().setAllLatelyOutCardTime();
		this.room.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
		
		//检查发牌前或者无庄家
		if (ZYPK_XuanZhuang.FaPai_Q.equals(this.room.xuanZhunag()) || ZYPK_XuanZhuang.Not.equals(this.room.xuanZhunag())) {
			this.state = SetState.Init;
			this.startMS = CommTime.nowMS();
		}
		
		//检查是否发牌后确认庄家
		if (ZYPK_XuanZhuang.FaPai_H.equals(this.room.xuanZhunag()) && ZYPK_XuanZhuang.Not.equals(this.xZhuang)) {
			//设置 当前一局的选庄状态 为发牌后确认庄家
			this.xZhuang = ZYPK_XuanZhuang.FaPai_H;
			this.startTimer();
		}

	}
	

	/**
	 * 洗牌
	 */
	private void xiPai () {
		//洗牌通知
		xuanZhuang();
		this.room.notify2All(SZYPK_Clean.make(this.room.getRoomID(), Op_KongPai.XiPai));
	}
	
	/**
	 * 收牌
	 */
	private void shouPai () {
		//清除公共牌
		this.publicCardList.clear();
		this.yaChouMa = 0;
		//清除所有人的私有牌
		ZYPKRoomPos<?> roomPos = null;
		for (int i = 0 ;i<this.room.getPlayerNum();i++) {
			if (!this.playerList.get(i)) continue;
			roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(i);
			roomPos.shouRoomPos(this.room.isAnNiu(ZYPK_AnNiu.KanPai));
		}
		this.room.notify2All(SZYPK_Clean.make(this.room.getRoomID(), Op_KongPai.ShouPai));
	}

	
	
	/**
	 * 玩家操作
	 * @param request
	 * @param onPlayerOp
	 */
	public void onPlayerOp (WebSocketRequest request, CZYPK_PlayerOp onPlayerOp) {
		//检查是否有操作实体
		if (null == onPlayerOp) {
			request.error(ErrorCode.NotAllow, "onPlayerOp not null");
			return; 
		}
		//检查玩家位置是否存在
		if (!checkPos(onPlayerOp.pos)) {
			request.error(ErrorCode.NotAllow, "onPlayerOp !checkPos(onPlayerOp.pos)");
			return; 
		}
		//检查是否可以操作
		if (!isOp(request,onPlayerOp.pos)) return;
		
		//获取玩家信息
		ZYPKRoomPos<?> roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(onPlayerOp.pos);
		if (null == roomPos) {
			request.error(ErrorCode.NotAllow, "roomPos not null");
			return;
		}

		
		Op_Player opPlayer = Op_Player.valueOf(onPlayerOp.opType);
		switch (opPlayer) {
		case Op:
			//操作按钮
			onAnNiu(request,opPlayer,onPlayerOp,roomPos);
			break;
		case Pass:
			//过
			if (null != this.curRound) {
				this.curRound.opCard(request, onPlayerOp.pos, opPlayer, ZYPK_AnNiu.Not, onPlayerOp.comporePos);
			}
			this.startNewRound();
			this.backupsMap.remove(roomPos.pid);
			pass();
			break;
		case Back:
			//撤回	
			if (this.backupsMap.containsKey(roomPos.pid)) {
				if (this.back(roomPos)) {
					if (null != this.curRound) {
						this.curRound.opCard(request, onPlayerOp.pos, opPlayer, ZYPK_AnNiu.Not, onPlayerOp.comporePos);
					}
				}
			}
			break;
		default:
			break;
		}
		request.response();
	}
	
	public void pass () {
		
	}
	
	/**
	 * 回退操作
	 * @param roomPos
	 * @return
	 */
	public boolean back (ZYPKRoomPos<?> roomPos) {
		ZYPK_Backups backups = this.backupsMap.get(roomPos.pid);
		if (null == backups)
			return false;
		roomPos.setPrivateCards(backups.getPrivateCards());
		roomPos.setOutCards(backups.getOutCards());
		roomPos.setMingBuCards(backups.getMingBuCards());
		roomPos.setBeiShu(backups.getBeiShu());
		roomPos.setYaZhu(backups.getYaZhu());
		roomPos.setoPpai(backups.getoPpai());
		roomPos.setQiPai(backups.isQiPai());
		roomPos.setKanPaiList(backups.getKanPaiList());
		Integer cardByte = backups.getCardBype();
		if (null == cardByte)
			return false;
		if (this.setCard == null)
			return false;
		this.setCard.addLeftCard(cardByte);
		return true;
		
	}
	
	
	/**
	 * 赠送筹码
	 * @param request
	 * @param onGive
	 */
	public void onGive(WebSocketRequest request, CZYPK_Give onGive){
		if (null == onGive) {
			request.error(ErrorCode.NotAllow, "onGive not null");
			return; 
		}		
		
		//获取自己玩家信息
		ZYPKRoomPos<?> roomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(onGive.pos);
		if (null == roomPos) {
			request.error(ErrorCode.NotAllow, "onGive roomPos not null");
			return;
		}
		
		//获取目标玩家信息
		ZYPKRoomPos<?> targetRoomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(onGive.targetPos);
		if (null == targetRoomPos) {
			request.error(ErrorCode.NotAllow, "onGive targetRoomPos not null");
			return;
		}
		
		if (onGive.chouMa < 0) {
			request.error(ErrorCode.NotAllow, "onGive onGive.chouMa < 0");
			return;
		}
		
		//扣除自己筹码
		if (roomPos.removeGive(onGive.chouMa)) {
			//赠送目标筹码
			targetRoomPos.getGive(onGive.chouMa);
			this.room.notify2All(SZYPK_Give.make(onGive.roomID, onGive.pos, onGive.chouMa, onGive.targetPos));
			request.response();
		} else {
			request.error(ErrorCode.NotAllow, "onGive targetRoomPos not null");
		}
	}
	
	/**
	 * 通用按钮
	 * @param request 
	 * @param onAnNiu
	 */
	public void onAnNiu(WebSocketRequest request,Op_Player opPlayer,CZYPK_PlayerOp onPlayerOp,ZYPKRoomPos<?> roomPos){
		//检查按钮的操作类型是否正确
		ZYPK_AnNiu anNiu = ZYPK_AnNiu.valueOf(onPlayerOp.anNiuType);
		if (ZYPK_AnNiu.Not.equals(anNiu)) {
			request.error(ErrorCode.NotAllow, "onOpenCard not ZYPK_AnNiu.Not");
			return;
		}

		//检查筹码或倍数 < 0
		if (onPlayerOp.addBet < 0) {
			request.error(ErrorCode.NotAllow, "onPlayerOp.addBet < 0");
			return;
		}
		this.backupsMap.put(roomPos.pid, this.newBackups(roomPos, anNiu));
		switch (anNiu) {
		case BuPai://补牌		
			this.buPai(request,roomPos,anNiu);
			break;
		case BuMingPai://补明牌
			this.buPai(request,roomPos,anNiu);
			break;
		case OutCard://出牌
			roomPos.deleteCard(onPlayerOp.cardList);
			break;
		case KanPai://看牌
			this.oPpai = Op_KanPai.Kan;
			roomPos.setoPpai(this.oPpai);
			break;
		case MingPai://明牌
			this.oPpai = Op_KanPai.Ming;
			roomPos.setoPpai(this.oPpai);
			break;
		case BiPai://比牌
			biPai(request,roomPos,onPlayerOp.comporePos);
			break;
		case GenZhu://跟注
			this.yaZhu(request,roomPos,onPlayerOp.addBet,anNiu);
			break;
		case YaZhu://压注
			this.yaZhu(request,roomPos,onPlayerOp.addBet,anNiu);
			break;
		case QiPai://弃牌
			roomPos.qiPai();
			break;
		case JiaBei://加倍
			roomPos.addJiaBei(onPlayerOp.addBet);
			break;
		default:
			break;
		}
		if (null != this.curRound) {
			this.curRound.opCard(request, onPlayerOp.pos, opPlayer, anNiu, onPlayerOp.comporePos);
		}
		request.response();
	}

	/**
	 * 补牌
	 * @param request 请求数据
	 * @param roomPos 玩家信息
	 * @param isMing 是否明牌
	 */
	private void buPai (WebSocketRequest request,ZYPKRoomPos<?> roomPos,ZYPK_AnNiu anNiu) {
		Integer cardByte = this.setCard.pop();
		if (null == cardByte) {
			//牌数已经用完。
			request.error(ErrorCode.NotAllow, "onOpenCard buPai null == cardByte");
			return;
		} else {
			ZYPK_Backups backups = this.backupsMap.get(roomPos.pid);
			if (null != backups) {
				backups.addCardBype(cardByte);
			}
			if (ZYPK_AnNiu.BuMingPai.equals(anNiu)) {
				roomPos.addMingPrivateCard(cardByte);
			} else {
				roomPos.addCard(cardByte);
			}
		}
	}
	
	/**
	 * 押注，跟注
	 * @param request
	 * @param roomPos
	 * @param addBet
	 * @param anNiu
	 */
	private void yaZhu (WebSocketRequest request,ZYPKRoomPos<?> roomPos,int addBet,ZYPK_AnNiu anNiu) {
		switch (anNiu) {
		case GenZhu:
			//跟注
			roomPos.addJiaZhu(this.yaChouMa);
			break;
		case YaZhu:
			//押注
			roomPos.addJiaZhu(addBet);
			this.yaChouMa = addBet;
			break;
		default:
			break;
		}
	}
	
	/**
	 * 比牌
	 * @param request
	 * @param roomPos
	 * @param comporePos
	 */
	private void biPai (WebSocketRequest request,ZYPKRoomPos<?> roomPos,int comporePos) {
		//获取玩家信息
		ZYPKRoomPos<?> comporeRoomPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(comporePos);
		if (null == comporeRoomPos) {
			request.error(ErrorCode.NotAllow, "comporeRoomPos not null");
			return;
		}
		roomPos.addKanPai(comporeRoomPos.pid);
		comporeRoomPos.addKanPai(roomPos.pid);		
	}
	
	/**
	 * 检查用户是否可以操作
	 * @param pos
	 * @return
	 */
	private boolean isOp (WebSocketRequest request,int pos) {
		if (null == this.curRound) {
			request.error(ErrorCode.NotAllow,"null == this.curRound");
			return false;
		}
		if (this.room.isLunLiu()) {
			if (pos != this.curRound.getOpPos()) {
				request.error(ErrorCode.NotAllow,"pos != this.curRound.getOpPos()");
				return false;
			}
			return true;
		} else {
			return true;
		}
	}
	
	
	/**
	 * 下一个位置
	 * @param opPos 位置
	 * @param isQiPai 是否弃牌
	 * @return
	 */
	public int nextOpPos (int opPos,boolean isQiPai) {
		int nextOpPos = 0;
		ZYPKRoomPos<?> zypkPos= null;
		for (int i = opPos ;i < this.room.getPlayerNum() ; i++) {
			nextOpPos = (i + 1) % this.room.getPlayerNum();
			if (this.getPlayerList().get(nextOpPos)) {
				if (isQiPai) {
					zypkPos = (ZYPKRoomPos<?>) this.room.getPosMgr().getPos(nextOpPos);
					if (zypkPos.getQiPai()) continue;
				}
				return nextOpPos;
			}
		}
		return nextOpPos;
	}

	
	/**
	 * 新建备份空间
	 * @param roomPos
	 * @return
	 */
	public ZYPK_Backups newBackups (ZYPKRoomPos<?> roomPos,ZYPK_AnNiu anNiu) {
		if (null == roomPos) 
			return null;
		ZYPK_Backups backups = new ZYPK_Backups();
		backups.setAnNiu(anNiu);
		backups.setBeiShu(roomPos.getBeiShu());
		backups.setKanPaiList(roomPos.getKanPaiList());
		backups.setMingBuCards(roomPos.getMingBuCards());
		backups.setoPpai(roomPos.getoPpai());
		backups.setOutCards(roomPos.getOutCards());
		backups.setPrivateCards(roomPos.getPrivateCards());
		backups.setQiPai(roomPos.isQiPai());
		backups.setYaZhu(roomPos.getYaZhu());
		return backups;
	}
}
