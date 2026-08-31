/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package business.player;

import BaseCommon.CommClass;
import BaseCommon.CommLog;
import BaseThread.BaseMutexInstance;
import BaseThread.BaseMutexObject;
import business.global.club.ClubMgr;
import business.global.family.Family;
import business.global.family.FamilyManager;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.feature.Feature;
import business.player.feature.PlayerWallet;
import business.rocketmq.bo.MqPlayerChangeNotifyBo;
import business.rocketmq.bo.MqPlayerPushProtoBo;
import business.rocketmq.bo.MqPlayerRemoveBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import cenum.LockLevelEnum;
import cenum.PlayerEnum.GMLEVEL;
import cenum.PlayerEnum.TRY_TO_PLAY_USERS;
import cenum.ShareDefine;
import cenum.VisitSignEnum;
import com.alibaba.druid.util.StringUtils;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.PlayerBO;
import core.db.entity.clarkGame.PlayerGPSBO;
import core.db.entity.dbZle.DbPopupBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerGPSBOService;
import core.db.service.dbZle.DbPopupBOService;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.phone.PhoneEvent;
import core.dispatch.event.player.LoginEvent;
import core.dispatch.event.player.LogoutEvent;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.client2game.ClientSession;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.LocationInfo;
import jsproto.c2s.cclass.Player.Property;
import jsproto.c2s.cclass.Player.PropertyLong;
import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.PlayerRequestRecordInfo;
import jsproto.c2s.iclass.S1010_SystemMessage;
import jsproto.c2s.iclass.SPlayerHeartBeat;
import jsproto.c2s.iclass.SPlayer_OnlineTime;
import lombok.Data;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
@Data
public class Player {
    /**
     * 访问标记
     * 用于大致定位玩家当前处于哪个界面
     */
    private VisitSignEnum signEnum = VisitSignEnum.NONE;

    /**
     * 在亲友圈页面的话 是哪个亲友圈页面
     */
    private long signEnumClubID = 0;
    /**
     * 俱乐部钻石消耗通知 全员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean diamondsAttentionAll;
    /**
     * 俱乐部钻石消耗通知 管理员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean diamondsAttentionMinister;
    /**
     * 赛事钻石消耗通知 全员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean unionDiamondsAttentionAll;
    /**
     * 赛事钻石消耗通知 管理员
     * 如果为true 表示已经通知过  不再下发通知
     */
    private boolean unionDiamondsAttentionMinister;
    /**
     * 玩家活跃积分
     */
    private final PlayerExp exp;
    /**
     * 玩家定位信息
     */
    private final LocationInfo locationInfo = new LocationInfo();
    /**
     * 玩家房间信息
     */
    private final PlayerRoomInfo roomInfo = new PlayerRoomInfo();
    /**
     * 玩家请求记录的信息
     */
    private final PlayerRequestRecordInfo playerRequestRecordInfo = new PlayerRequestRecordInfo();

    // 游戏列表
    private List<Integer> gameList = new ArrayList<Integer>();

    final protected BaseMutexObject m_mutex = new BaseMutexObject();
    /**
     * 当前锁等级 10级
     */
    final protected BaseMutexInstance m_mutexIns = new BaseMutexInstance();
    // 玩家UUID
    private volatile String uUID = "";
    // 用户行为锁
    private String ip = "";
    private int hourTime = 0;
    private long lastTime = 0L;
    private int isMobile = 0;
    /**
     * 当天首次登陆
     */
    private boolean isTodayFirstLogin = false;

    public void lockIns() {
        m_mutexIns.lock();
    }

    //
    public void unlockIns() {
        m_mutexIns.unlock();
    }

    public void lock() {
        m_mutex.lock();
    }

    //
    public void unlock() {
        m_mutex.unlock();
    }

    private final PlayerBO playerBO;
    private final boolean isVPlayer;
    /**
     * 是否允许邀请玩家
     */
    private boolean inviteFlag = true;

    public Player(PlayerBO playerBO) {
        this.playerBO = playerBO;
        this.exp = new PlayerExp(playerBO);
        long accountID = playerBO.getAccountID();
        if (accountID == 0) {
            this.isVPlayer = true;
        } else {
            this.isVPlayer = false;
        }
        playerBO.setName(StringUtil.regexMobile(playerBO.getName()));
        // 当前锁等级32级
        m_mutex.reduceMutexLevel(LockLevelEnum.LEVLE_2.value());
    }

    /**
     * 不在房间中
     *
     * @return
     */
    public boolean notExistRoom() {
        //共享的情况
        if (Config.isShare()) {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(this.getPid());
            if (Objects.isNull(sharePlayer)) {
                return true;
            }
            if (Objects.isNull(sharePlayer.getRoomInfo()) || sharePlayer.getRoomInfo().getRoomId() <= 0) {
                return true;
            }
            // 在房间中
            return !VisitSignEnum.ROOM.equals(sharePlayer.getSignEnum());
        } else {
            if (Objects.isNull(getRoomInfo()) || getRoomInfo().getRoomId() <= 0) {
                return true;
            }
            // 在房间中
            return !VisitSignEnum.ROOM.equals(signEnum);
        }
    }

    /**
     * 设置亲友圈所在的clubID
     *
     * @param signEnum
     */
    public void setSignEnum(VisitSignEnum signEnum) {
        this.signEnum = signEnum;
        if (!VisitSignEnum.CLUN_ROOM_MAIN.equals(signEnum)) {
            this.setSignEnumClubID(0L);
            //更新共享玩家
            if (Config.isShare()) {
                SharePlayerMgr.getInstance().updateField(this, "signEnum", "signEnumClubID");
            }
        }
    }

    /**
     * 更新玩家信息
     *
     * @param playerBO
     */
    public void updatePlayer(PlayerBO playerBO) {
        this.playerBO.setFamilyID(playerBO.getFamilyID());
    }

    /**
     * 在线时间+1
     */
    public synchronized void addHourTime() {
        hourTime++;
    }

    /**
     * 清空在线小时
     *
     * @return
     */
    public synchronized void cleanHourTime() {
        this.hourTime = 0;
    }

    public synchronized int getHourTime() {
        return hourTime;
    }

    public synchronized void setHourTime(int hourTime) {
        this.hourTime = hourTime;
    }


    /**
     * 获取游戏列表
     *
     * @return
     */
    public String getGameList() {
        return gameList.toString();
    }

    /**
     * 清除游戏列表
     */
    public void clearGameList() {
        gameList.clear();
    }

    // 需要被抛弃的接口
    public long getPid() {
        return playerBO.getId();
    }

    // 新接口
    public long getId() {
        return playerBO.getId();
    }

    public long getAccountID() {
        return playerBO.getAccountID();
    }


    public String getName() {
        return playerBO.getName();
    }

    public String getHeadImageUrl() {
        return playerBO.getHeadImageUrl();
    }

    public long getFamiliID() {
        return playerBO.getFamilyID();
    }

    /**
     * 设置IP地址
     *
     * @param ip
     * @return
     */
    public boolean setIp(String ip) {
        // 检查是否为null
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        // 检查IP格式是否正确
        if (!StringUtil.isboolIp(ip)) {
            return false;
        }
        this.ip = ip;
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "ip");
        }
        return true;
    }

    public void setCurrentGameType(int currentGameType) {
        playerBO.savecurrentGameType(currentGameType);
//		//更新共享玩家
//		if(Config.isShare()){
//			SharePlayerMgr.getInstance().updateField(playerBO, "currentGameType");
//		}
    }

    public void cleanCurrentGameType() {
        playerBO.savecurrentGameType(-1);
//		//更新共享玩家
//		if(Config.isShare()){
//			SharePlayerMgr.getInstance().updateField(playerBO, "currentGameType");
//		}
    }

    public void setRealPlayer(String realName, String realNumber) {
        playerBO.saveRealName(realName);
        playerBO.saveRealNumber(realNumber);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(playerBO, "realName", "realNumber");
        }
        // getFeature(PlayerTask.class).exeTask(TaskTargetType.Certification.ordinal());

    }

    /**
     * 是否游客
     *
     * @return
     */
    public boolean isTourist() {
        return playerBO.getIcon() > 0;
    }

    /**
     * 获取玩家房卡数量
     *
     * @return
     */
    public int getRoomCard() {
        return getFeature(PlayerWallet.class).balance();
    }

    public boolean isVirtualPlayer() {
        return isVPlayer;
    }

    /**
     * 设置玩家登陆状态
     *
     * @param bannedInt
     * @return
     */
    public boolean setBannedLogin(int bannedInt) {
        playerBO.saveBannedLoginExpiredTime(bannedInt);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(playerBO, "bannedLoginExpiredTime");
        }
        var bannedEvent=new MqPlayerRemoveBo(this.getPid());
        var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.PLAYER_BANNED_LOGIN_NOTIFY,"ban-login:"+getPid()+":"+bannedInt,bannedEvent);
        if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED) CommLogD.error("ban-login MQ unconfirmed pid:{}, result:{}",getPid(),confirmation);
        return true;
    }

    /**
     * 检查是否禁止登陆
     *
     * @return
     */
    public boolean isBannedLogin(WebSocketRequest request) {
        // 检查玩家身上是否有游戏
        long roomID = getRoomInfo().getRoomId();
        // 如果房间ID > 0 ,证明有游戏
        if (roomID > 0) {
            // 找到游戏房间，检查是否存在。
            AbsBaseRoom baseRoom = RoomMgr.getInstance().getRoom(roomID);
            // 检查房间是否存。
            if (null != baseRoom) {
                return false;
            }
        }
        // 获取禁封时间
        int expiredTime = playerBO.getBannedLoginExpiredTime();
        // 检查是否禁止封号
        boolean isBanned = expiredTime == -1 || expiredTime > CommTime.nowSecond();
        if (isBanned) {
            request.error(ErrorCode.Banned_Login, String.valueOf(expiredTime));
        }
        return isBanned;
    }

    // 更新玩家头像URL
    public void updateHeadImageUrl(String newHeadImageUrl, String nickName) {
        try {
            this.lock();
            if (!StringUtils.isEmpty(nickName)) {
                this.playerBO.saveName(StringUtil.regexMobile(nickName));
            }
            if (!StringUtils.isEmpty(newHeadImageUrl) && StringUtil.existHttpUrl(newHeadImageUrl)) {
                this.playerBO.saveHeadImageUrl(newHeadImageUrl);
            }
            //更新共享玩家
            if (Config.isShare()) {
                SharePlayerMgr.getInstance().updateField(this.playerBO, "name", "headImageUrl");
            }
        } finally {
            this.unlock();
        }

    }

    // 更新玩家昵称
    public void updateNickName(String nickName) {

        if (nickName.length() == 0) {
            return;
        }
        try {
            this.lock();
            this.playerBO.setName(nickName);
        } finally {
            this.unlock();
        }
    }

    // 组件列表
    private Hashtable<String, Feature> features = new Hashtable<>();

    public boolean isLoaded(Class<? extends Feature> clazz) {
        return features.get(clazz.getName()) != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeature(Class<T> clazz) {
        Feature feature = getFeature(clazz.getName());
        feature.tryLoadDBData();
        return (T) feature;
    }

    private Feature getFeature(String featureName) {
        Feature feature = features.get(featureName);
        if (feature != null) {
            feature.updateLastActiveTime();
            return feature;
        }
        synchronized (features) {
            feature = features.get(featureName);
            if (feature != null) {
                return feature;
            }
            try {
                Class<?> cs = CommClass.forName(featureName);
                @SuppressWarnings("unchecked")
                Constructor<Feature> constructor = (Constructor<Feature>) cs.getConstructor(Player.class);
                feature = constructor.newInstance(this);
                features.put(featureName, feature);
                return feature;
            } catch (Exception e) {
                CommLogD.error(e.getMessage(), e);
                System.exit(-1);
                return null;
            }
        }
    }

    /**
     * 释放组件
     *
     * @param activeTime --保持活跃留存的秒数-当前为一天时间的秒数
     */
    public void releaseFeature(int activeTime) {
        int curTime = CommTime.nowSecond();

        // 不是正常玩家，不处理
        if (this.isVirtualPlayer()) {
            return;
        }

        // 在线的不处理
        if (isOnline()) {
            return;
        }

        // activeTime时间内使用过，先不处理
        if (getPlayerBO().getLastLogin() + activeTime > curTime) {
            return;
        }

        try {
            lockIns();
            for (Feature feature : new ArrayList<>(features.values())) {
                // activeTime时间内使用过，先不处理
                if (feature.getLastActiveTime() + activeTime > curTime) {
                    continue;
                }
                features.remove(feature.getClass().getName());
            }
        } catch (Exception e) {
            CommLogD.error("释放玩家Feature时发生异常", e);
        } finally {
            unlockIns();
        }

    }

    // ============================== 网络相关 ====================================
    private ClientSession session;

    public ClientSession getClientSession() {
        return session;
    }

    public boolean isOnline() {
        return session != null;
    }

    public void bindSession(ClientSession session) {

        lockIns();
        // 连接到来
        this.session = session;
        unlockIns();
        // 最近时间清空
        this.setLastTime(CommTime.nowMS());
        // 心跳通知
        this.heartBeatPushProto();
        // 清空在线小时
        this.cleanHourTime();
        // 标识玩家在线
        PlayerMgr.getInstance().regOnlinePlayer(this);
        DispatcherComponent.getInstance().publish(new LoginEvent(false, this));
        if (getPlayerBO().getFastCard() != 1 && getPlayerBO().getPhone() > 0L) {
            DispatcherComponent.getInstance().publish(new PhoneEvent(this));
        }

    }


    public void uuidBindSession(ClientSession session) {
        lockIns();
        // 连接到来
        this.session = session;
        unlockIns();
        // 心跳通知
        this.heartBeatPushProto();
        // 标识玩家在线
        PlayerMgr.getInstance().regOnlinePlayer(this);
        DispatcherComponent.getInstance().publish(new LoginEvent(true, this));
//		if (getRoomInfo().getRoomId() > 0L) {
//			DispatcherComponent.getInstance().publish(new PhoneEvent(true, this));
//		}
    }


    /**
     * 如果登录时间不是今天，并且不是游客，则插入记录。
     */
    public void playerDataLog() {
        // 如果登录时间不是今天，并且不是游客，则插入记录。
        if (!CommTime.isSameDayWithInTimeZone(this.playerBO.getLastLogin(), CommTime.nowSecond()) && !this.isTourist()) {
            FlowLogger.playerDataLog(this.playerBO.getId(), this.getAccountID(), this.playerBO.getCreateTime(),
                    TRY_TO_PLAY_USERS.NEW_USER.value(), 0, 0);
        } else if (!this.isTourist()) {
            if (CommTime.minTimeDifference(this.playerBO.getLastLogin(), CommTime.nowSecond()) >= 8) {
                FlowLogger.playerDataLog(this.playerBO.getId(), this.getAccountID(), this.playerBO.getCreateTime(),
                        TRY_TO_PLAY_USERS.NEW_USER.value(), 1, 0);
            }
        }
    }

    public void loseSession() {
        PlayerMgr.getInstance().unregOnlinePlayer(this);
        session = null;
        // 清空在线小时
        this.cleanHourTime();
        // 清除游戏列表
        this.clearGameList();
        DispatcherComponent.getInstance().publish(new LogoutEvent(this.getPid(), this));
    }


    public void uuidLoseSession() {
        PlayerMgr.getInstance().unregOnlinePlayer(this);
        session = null;
    }


    /**
     * 设置玩家定位信息
     */
    public void setLocationInfo(String address, double latitude, double longitude, boolean isGetError) {
        // 检查定位是否成功！
        if (isGetError || latitude <= 0D || longitude <= 0D) {
            // 定位失败
            // 定位更新时间相距大于等60分钟。
            int minute = CommTime.MinutesBetween(CommTime.nowMS(), CommTime.SecToMsec(this.getLocationInfo().getUpdateTime()));
            if (minute >= 60) {
                CommLog.info("error clear setLocationInfo Pid:{},isGetError:{},minute:{},locationInfo:{}", getPid(), isGetError, minute, locationInfo.toString());
                // 清空定位信息
                this.locationInfo.clear(CommTime.nowSecond());
            }
//			else {
//				CommLog.info("error setLocationInfo Pid:{},isGetError:{},minute:{},locationInfo:{}",getPid(),isGetError,minute,locationInfo.toString());
//			}
            //更新共享玩家
            if (Config.isShare()) {
                SharePlayerMgr.getInstance().updateField(this, "locationInfo");
//				SharePlayerMgr.getInstance().updateAllSharePlayer(this);
            }
            return;
        }
        // 获取GPS时间
        int gpsTime = this.locationInfo.getUpdateTime();
        this.locationInfo.setAddress(address);
        this.locationInfo.setGetError(false);
        this.locationInfo.setLatitude(latitude);
        this.locationInfo.setLongitude(longitude);
        this.locationInfo.setPid(getPid());
        this.locationInfo.setUpdateTime(CommTime.nowSecond());
//		CommLog.info("setLocationInfo Pid:{},isGetError:{},locationInfo:{}",getPid(),isGetError,locationInfo.toString());
        if (!CommTime.isSameDayWithInTimeZone(gpsTime, CommTime.nowSecond())) {
            // 添加GPS
            this.insertPlayerGPS(latitude, longitude);
        }
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this, "locationInfo");
        }
    }

    /**
     * 更新插入GPS
     *
     * @param latitude
     * @param longitude
     */
    private void insertPlayerGPS(double latitude, double longitude) {
        PlayerGPSBO gps = ContainerMgr.get().getComponent(PlayerGPSBOService.class).findOne(Restrictions.eq("pid", this.getPid()), "");
        if (null == gps) {
            gps = new PlayerGPSBO();
        }
        gps.setPid(this.getPid());
        gps.setPlayerTime((int) (this.getPlayerBO().getCreateTime() / 1000));
        gps.setIp(this.ip);
        gps.setLatitude(String.valueOf(latitude));
        gps.setLongitude(String.valueOf(longitude));
        gps.setFamilyTime(familyTime());
        gps.setCreateTime(CommTime.nowSecond());
        gps.setUpdateTime(CommTime.nowSecond());
        gps.getBaseService().saveOrUpDate(gps);
    }

    /**
     * 获取代理时间
     *
     * @return
     */
    private int familyTime() {
        if (this.getFamiliID() == Family.DefaultFamilyID || getFamiliID() <= 0) {
            return 0;
        }
        Family family = FamilyManager.getInstance().getFamily(getFamiliID());
        if (null == family) {
            return 0;
        }
        // 检查公会会长是否本玩家
        if (this.getPid() == family.getOwnerID()) {
            return (int) family.getFamilyBO().getCreateTime();
        }
        return 0;
    }


    // ============================== 网络相关 ====================================
    public void pushProto(BaseSendMsg msg) {
        if (Objects.isNull(msg.getSignEnum()) || msg.getSignEnum().equals(this.getSignEnum())) {
            pushProto(msg.getOpName(), msg);
        }
    }

    public void pushProto(BaseSendMsg msg, String pubTopic) {
        if (Objects.isNull(msg.getSignEnum()) || msg.getSignEnum().equals(this.getSignEnum())) {
            pushProto(msg.getOpName(), msg, pubTopic);
        }
    }

    /**
     * mq通知
     *
     * @param msg
     */
    public void pushProtoMq(BaseSendMsg msg) { pushProtoMqConfirmed(msg); }

    public MqProducerMgr.Confirmation pushProtoMqConfirmed(BaseSendMsg msg) {
        String businessKey="player-push:"+getPid()+":"+msg.getClass().getName()+":"+msg.hashCode();
        var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.PLAYER_PUSH_PROTO,businessKey, new MqPlayerPushProtoBo<>(this.getPlayerBO().getId(), msg, msg.getClass().getName()));
        if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED) CommLogD.error("player push MQ not confirmed pid:{}, result:{}",getPid(),confirmation);
        return confirmation;
    }

    public void pushProto(String operation, Object proto) {
        if (session != null && session.isPrint()) {
            CommLogD.error(String.format("[notify][%s]：%s", operation, proto.toString()));
        }

        if (session != null) {
            session.notifyMessage(operation, proto);
        }
    }

    public void pushProto(String operation, Object proto, String pubTopic) {
        if (session != null && session.isPrint()) {
            CommLogD.error(String.format("[notify][%s]：%s", operation, proto.toString()));
        }
        if (session != null) {
            session.notifyMessage(operation, proto, pubTopic);
        }
    }

    public void pushProperties(String name, int value) {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property(name, value));
        this.pushProperties(properties);
    }

    public void pushProperties(String name, long value) {
        List<PropertyLong> properties = new ArrayList<>();
        properties.add(new PropertyLong(name, value));
        this.pushLongProperties(properties);
    }

    public void pushProperties(String name0, int value0, String name1, int value1) {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property(name0, value0));
        properties.add(new Property(name1, value1));
        this.pushProperties(properties);
    }

    public void pushProperties(List<Property> properties) {
        if (Config.isShare()) {
            MqProducerMgr.get().send(MqTopic.PLAYER_CHANGE_NOTIFY, new MqPlayerChangeNotifyBo(this.getPid(), properties));
        } else {
            this.pushProto("playerchanged", properties);
        }
    }

    public void pushPropertiesMq(List<Property> properties) {
        this.pushProto("playerchanged", properties);
    }

    public void pushLongProperties(List<PropertyLong> properties) {
        this.pushProto("playerchanged", properties);
    }

    public void pushMessage(String key, String... params) {
        ArrayList<String> p = new ArrayList<>();
        for (String s : params) {
            p.add(s);
        }
        pushProto("S1010_SystemMessage", S1010_SystemMessage.make(key, p));
    }

    public void pushProperties() {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("gold", this.getPlayerBO().getGold()));
        properties.add(new Property("roomCard", getRoomCard()));
        this.pushProperties(properties);
    }


    /**
     * 试玩用户
     *
     * @return
     */
    public void tryToPlayUsers() {
        // 检查用户是否试玩
        if (this.playerBO.getVipLevel() > 0) {
            return;
        }
        // 检查如果是游客
        if (this.isTourist()) {
            return;
        }
        // 获取秒级时间戳
        int createPlayerTime = (int) (this.playerBO.getCreateTime() / 1000);
        // 检查是否同一天注册并玩房卡游戏。
        if (CommTime.isSameDayWithInTimeZone(createPlayerTime, CommTime.nowSecond())) {
            // 设置同一天
            this.playerBO.saveVipLevel(TRY_TO_PLAY_USERS.CUR_TRIAL_PLAY.value());
            FlowLogger.playerDataLog(this.playerBO.getId(), this.getAccountID(), this.playerBO.getCreateTime(),
                    TRY_TO_PLAY_USERS.CUR_TRIAL_PLAY.value(), 0, 0);
        } else {
            this.playerBO.saveVipLevel(TRY_TO_PLAY_USERS.NOT_TRY_TO_PLAY.value());
        }
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this.playerBO, "vipLevel");
        }
    }


    /**
     * 是否拥有GM权限
     *
     * @return
     */
    public boolean isGmLevel() {
        if (this.playerBO.getGmLevel() >= GMLEVEL.GMLEVEL_LEVEL_ONE.value()) {
            return true;
        }
        return false;
    }

    public boolean isGmLevelEx() {
        if (this.playerBO.getGmLevel() >= GMLEVEL.GMLEVEL_LEVEL_TWO.value()) {
            return true;
        }
        return false;
    }


    /**
     * 获取玩家基本信息
     *
     * @return
     */
    public ShortPlayer getShortPlayer() {
        ShortPlayer ret = new ShortPlayer();
        ret.setName(this.playerBO.getName());
        ret.setPid(getPid());
        ret.setIconUrl(this.playerBO.getHeadImageUrl());
        ret.setAccountID(this.playerBO.getAccountID());
        return ret;
    }

    /**
     * 没有头像和账号Id
     *
     * @return
     */
    public ShortPlayer getUpShortPlayer() {
        ShortPlayer ret = new ShortPlayer();
        ret.setName(this.playerBO.getName());
        ret.setPid(getPid());
        return ret;
    }

    /**
     * 生成新的UUID，并获取。
     *
     * @return
     */
    public String getuUID(String gameName) {
        if (StringUtils.isEmpty(this.getUUID())) {
            CommLog.error("Player reconnect token is empty, pid:{}", this.getPid());
            return "";
        }
        return this.getUUID();
    }

    /**
     * 设置UUID
     *
     * @param uUID
     */
    public void setUUID(String uUID) {
        if (StringUtils.isEmpty(uUID)) {
            CommLog.error("Refusing to set an empty reconnect token, pid:{}", this.getPid());
            return;
        }
        this.uUID = uUID;
    }

    /**
     * 获取UUID
     *
     * @return
     */
    public String getUUID() {
        return uUID;
    }

    /**
     * 检查玩家uUID
     *
     * @param uUID
     * @return
     */
    public boolean checkuUID(String uUID) {
        // 检查 uUID是否正常，
        if (StringUtils.isEmpty(uUID)) {
            CommLog.error("checkuUID isEmpty true uUID");
            return false;
        }
        if (StringUtils.isEmpty(this.getUUID())) {
            CommLog.error("checkuUID isEmpty true getUUID");
            return false;
        }
        if (uUID.toLowerCase().equals(this.uUID.toLowerCase())) {
            return true;
        }
        CommLog.error("Reconnect token mismatch, pid:{}", this.getPid());
        return false;
    }


    /**
     * 断开连接
     *
     * @return
     */
    public boolean disconnect() {
        if (getClientSession() != null) {
            // 旧的连接，被替换
            getClientSession().losePlayer();
        }
        // 旧的账号， 失去连接
        this.loseSession();
        return true;
    }


    /**
     * 获取UUID断开连接
     *
     * @return
     */
    public boolean uuidDisconnect() {
        if (getClientSession() != null) {
            // 旧的连接，被替换
            getClientSession().uuidLosePlayer();
        }
        // 旧的账号， 失去连接
        this.uuidLoseSession();
        return true;
    }


    /**
     * 玩家进入房间 标识进入的房间ID
     *
     * @param roomID 房间ID
     * @return
     */
    public boolean onEnterRoom(long roomID) {
        try {
            lock();
            // 检查玩家身上的房间ID是否存在。
            if (0 != this.getRoomInfo().getRoomId()) {
                //更新共享玩家
                if (Config.isShare()) {
//				SharePlayerMgr.getInstance().updateAllSharePlayer(this);
                    SharePlayerMgr.getInstance().updateField(this, "roomInfo");
                }
                return false;
            }
            // 设置玩家身上的房间ID
            this.getRoomInfo().setRoomId(roomID);
            // 设置标记
            this.setSignEnum(VisitSignEnum.ROOM);
            //更新共享玩家
            if (Config.isShare()) {
//				SharePlayerMgr.getInstance().updateAllSharePlayer(this);
                SharePlayerMgr.getInstance().updateField(this, "signEnum", "signEnumClubID", "roomInfo");
            }
            return true;
        } finally {
            unlock();
        }
    }

    /**
     * 玩家离开房间 清除玩家在当前房间的标识
     *
     * @param roomID 房间ID
     */
    public boolean onExitRoom(long roomID) {
        try {
            lock();
            if (this.getRoomInfo().getRoomId() == roomID) {
                boolean isClubOrUnion = this.getRoomInfo().checkClubOrUnion();
                // 清空房间信息
                this.getRoomInfo().clear();
                // 清空标记
                this.setSignEnumClubID(this.getRoomInfo().getClubId());
                this.setSignEnum(isClubOrUnion ? VisitSignEnum.CLUN_ROOM_MAIN : VisitSignEnum.NONE);
                //更新共享玩家
                if (Config.isShare()) {
//					SharePlayerMgr.getInstance().updateAllSharePlayer(this);
                    SharePlayerMgr.getInstance().updateField(this, "roomInfo", "signEnum", "signEnumClubID");
                }
                return true;
            }
            return false;
        } finally {
            unlock();
        }
    }

    /**
     * 强制清空用户的游戏状态
     */
    public void onGMExitRoom() {
        long rId = this.getRoomInfo().getRoomId();
        if (this.getRoomInfo().getRoomId() > 0L) {
            // 查询玩家标识的房间ID是否存在
            AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomInfo().getRoomId());
            if (null != room) {
                // 存在，解散该房间。
                room.doDissolveRoom();
            }
        }
        boolean isClubOrUnion = this.getRoomInfo().checkClubOrUnion();
        // 清空房间信息
        this.getRoomInfo().clear();
        // 清除玩家当前游戏标识
        cleanCurrentGameType();
        // 清空标记
        this.setSignEnumClubID(this.getRoomInfo().getClubId());
        this.setSignEnum(isClubOrUnion ? VisitSignEnum.CLUN_ROOM_MAIN : VisitSignEnum.NONE);
        CommLogD.error("OK clean player room , roomID : {},playerId : {}", rId, this.getId());
        //更新共享玩家
        if (Config.isShare()) {
//			SharePlayerMgr.getInstance().updateAllSharePlayer(this);
            SharePlayerMgr.getInstance().updateField(this, "roomInfo", "signEnumClubID", "signEnum");
        }
    }


    /**
     * 获取是否赠送房卡
     */
    public long getSendClubReward() {
        return this.playerBO.getSendClubReward();
    }

    /**
     * 保存赠送房卡的值
     */
    public void saveSendClubReward(long sendClubReward) {
        this.playerBO.saveSendClubReward(sendClubReward);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(this.playerBO, "sendClubReward");
        }
    }

    /**
     * 推送活动弹窗列表
     *
     * @return
     */
    public Optional<List<DbPopupBO>> pushPopupList(int type) {
        //crowdtype = 4 的亲友圈手动过滤
        Optional<List<DbPopupBO>> dbPopupBOList = ContainerMgr.get().getComponent(DbPopupBOService.class).findAllByTime(type == 1 ? isTodayFirstLogin : true, type == 1 ? false : true, playerBO.getFamilyID());
        if (dbPopupBOList.isPresent()) {
            Optional<List<Long>> joinedClubIds = ClubMgr.getInstance().findAllJoinClubByPid(getPid());
            List<DbPopupBO> filterDbPopupBOList = dbPopupBOList.orElseThrow().stream().filter(dbPop -> {
                //指定亲友圈
                if (dbPop.getCrowdtype() == 4) {
                    //切分弹窗限定的亲友圈列表
                    List<String> crowList = Arrays.asList(dbPop.getCrowdlist().split(","));
                    return joinedClubIds.map(clubs -> clubs.stream()
                            .anyMatch(club -> crowList.contains(String.valueOf(club)))).orElse(false);
                }
                return true;
            }).collect(Collectors.toList());
            return Optional.ofNullable(filterDbPopupBOList);
        }
        return Optional.empty();
    }


    /**
     * 心跳通知下发
     */
    public void heartBeatPushProto() {
        SPlayerHeartBeat heart = new SPlayerHeartBeat();
        heart.strBeatMessage = "heart beat normal.";
        heart.bBeatFlag = true;
        heart.lBeatTime = CommTime.nowMS();
        pushProto(heart.getOpName(), heart);
    }

    /**
     * 心跳通知
     */
    public void heartBeat() {
        long last = getLastTime();
        if (last != 0) {
            if (CommTime.nowMS() - last > ShareDefine.MAXHEARBEATINTERVAL) {
                loseSession();
            } else {
                heartBeatPushProto();
//				setLastTime(CommTime.nowMS());
            }
        } else if (last == 0) {
            heartBeatPushProto();
//			setLastTime(CommTime.nowMS());
        }
    }

    /**
     * 检查是否有有效的定位数据
     *
     * @return
     */
    public boolean checkLocationInfo() {
        if (null == this.getLocationInfo()) {
            return false;
        }
        return !this.getLocationInfo().isGetError();
    }

    /**
     * 记录玩家在线时间
     */
    public void hourTimePushProto() {
        this.addHourTime();
        if (this.getHourTime() == 3) {
            this.pushProto(SPlayer_OnlineTime.make(3));
        } else if (this.getHourTime() == 8) {
            this.pushProto(SPlayer_OnlineTime.make(8));
        }
    }

    /**
     * 检查并增加记录时间
     *
     * @param handler        请求接口头部
     * @param requestNowTime 当前请求时间
     * @return
     */
    public boolean checkAndAddRequestConcurrentRecor(String handler, long requestNowTime) {
        return this.playerRequestRecordInfo.checkAndAddRequestConcurrentRecor(handler, requestNowTime);
    }

    /**
     * 存在超过并发次数限制
     *
     * @return
     */
    public boolean existExceededConcurrencyLimit(String handler) {
        return this.playerRequestRecordInfo.existExceededConcurrencyLimit(handler);
    }


    /**
     * 分钟内出现超高频率请求次数
     *
     * @return
     */
    public int getMinuteUltrahighFrequencyCount(long requestNowTime) {
        return this.playerRequestRecordInfo.getMinuteUltrahighFrequencyCount(requestNowTime);
    }

    /**
     * 1秒内请求超时的接口出现 10次则踢出用户
     *
     * @param overTime
     * @return
     */
    public boolean existOvertimeInterface(long overTime) {
        return this.playerRequestRecordInfo.existOvertimeInterface(overTime);
    }

    public int getCurrentGameType() {
        return this.roomInfo.getRoomId() <= 0L ? -1 : this.getPlayerBO().getCurrentGameType();
    }
}
