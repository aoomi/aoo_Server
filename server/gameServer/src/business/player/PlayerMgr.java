package business.player;

import BaseCommon.CommLog;
import BaseThread.BaseMutexObject;
import business.global.club.ClubMgr;
import business.global.family.Family;
import business.global.sharegm.ShareInitMgr;
import business.player.feature.PlayerWallet;
import business.player.feature.PlayerFamily;
import business.shareplayer.SharePlayerMgr;
import cenum.ItemFlow;
import cenum.LockLevelEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.PlayerBO;
import core.db.service.clarkGame.PlayerBOService;
import core.ioc.ContainerMgr;
import core.network.client2game.ClientSession;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.iclass.CPlayer_GetPlayerInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public class PlayerMgr {

    private PlayerBOService playerBOService;

    // 在线玩家<pid,player>
    public final Map<Long, Player> allOnLinePlayerMap = Maps.newConcurrentMap();
    // 所有玩家<pid,player>
    public final Map<Long, Player> allPlayerMap = Maps.newConcurrentMap();
    // 所有玩家<name, playerID>
    public final Map<String, Long> allNamePlayerIDMap = Maps.newConcurrentMap();
    // 素有玩家<accountID, playerID>
    public final Map<Long, Long> allAccountIDPlayerIDMap = Maps.newConcurrentMap();
    // 等待创角连接<accountID, session>
    public final Map<Long, ClientSession> allWaitCreateMap = Maps.newConcurrentMap();
    // 锁标志
    private final BaseMutexObject _lock = new BaseMutexObject();
    // 获取在线人数
    private final AtomicInteger onlinePlayerSize = new AtomicInteger();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    private static PlayerMgr instance = new PlayerMgr();

    public static PlayerMgr getInstance() {
        return instance;
    }

    private PlayerMgr() {
        playerBOService = ContainerMgr.get().getComponent(PlayerBOService.class);
        // 当前锁等级30级
        _lock.reduceMutexLevel(LockLevelEnum.LEVLE_1.value());
    }

    // 开服初始化
    public void init() {
        CommLog.info("[PlayerMgr.init] load player begin...]");

        lock();

        List<PlayerBO> allPlayers = playerBOService.findAll(null, "");
//		List<PlayerBO> allPlayers = BM.getBM(PlayerBO.class).findAll();
        for (PlayerBO bo : allPlayers) {
            regPlayer(new Player(bo), ShareInitMgr.getInstance().getShareDataInit());
        }
        unlock();

        CommLog.info("[PlayerMgr.init] load player success, count: {}", allPlayerMap.size());
    }

    // 新增玩家对象
    public void regPlayer(Player player, boolean saveShare) {
        lock();
        PlayerBO playerBO = player.getPlayerBO();
        long playerID = playerBO.getId();
        String playerName = playerBO.getName();
        long accountID = playerBO.getAccountID();

        if (allPlayerMap.get(playerID) != null) {
            CommLogD.warn("regPlayer({}) allPlayerMap have find", playerID);
        }
        allPlayerMap.put(playerID, player);

        if (allNamePlayerIDMap.containsKey(playerName)) {
            CommLogD.warn("regPlayer({},{}) allNamePlayerIDMap have find ({})", playerName, playerID,
                    allNamePlayerIDMap.get(playerName));
        }
        allNamePlayerIDMap.put(playerName, playerID);

        // 机器人不追加
        if (accountID != 0) {
            if (allAccountIDPlayerIDMap.containsKey(accountID)) {
                CommLogD.warn("regPlayer({},{}) allAccountIDPlayerIDMap have find ({})", accountID, playerID,
                        allAccountIDPlayerIDMap.get(accountID));
            }
            allAccountIDPlayerIDMap.put(accountID, playerID);
        }
        //添加共享玩家
        if (Config.isShare() && saveShare) {
            SharePlayerMgr.getInstance().addAllSharePlayer(player);
        }

        unlock();
    }

    // 注册在线玩家
    public void regOnlinePlayer(Player player) {
        long playerID = player.getId();
        // 检查用户ID 是否存在
        if (this.allOnLinePlayerMap.putIfAbsent(playerID, player) == null) {
            this.onlinePlayerSize.incrementAndGet();
        } else {
            this.allOnLinePlayerMap.put(playerID, player);
        }
        //添加共享在线玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().addOnlineSharePlayer(player);
        }
    }

    // 卸载在线玩家
    public void unregOnlinePlayer(Player player) {
        long playerID = player.getId();
        // 检查是否移除成功
        if (this.allOnLinePlayerMap.remove(playerID) != null) {
            // 如果在线人数 > 0
            this.onlinePlayerSize.updateAndGet(size -> Math.max(0, size - 1));
        }
        //删除共享在线玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().removeOnlineSharePlayer(player);
        }
    }

    /**
     * 获取在线玩家
     *
     * @param pid
     * @return
     */
    public Player getOnlinePlayerByPid(long pid) {
        return allOnLinePlayerMap.get(pid);

    }

    /**
     * 检查存在指定的Pid在线
     *
     * @param pid
     * @return
     */
    public boolean checkExistOnlinePlayerByPid(long pid) {
        //从共享里面获取
        if (Config.isShare()) {
            return SharePlayerMgr.getInstance().checkSharePlayerByOnline(pid);
        } else {
            return this.allOnLinePlayerMap.containsKey(pid);
        }
    }

    public boolean havePlayer(long playerID) {
        Player player = allPlayerMap.get(playerID);
        if (player == null) {
            return false;
        }
        return true;
    }

    /**
     * 获取玩家信息
     *
     * @param playerID
     * @return
     */
    public Player getPlayer(long playerID) {
        Player player = allPlayerMap.get(playerID);
        if (player == null) {
            // CommLogD.error("getPlayer({}) not find player", playerID);
        }
        //从共享里面设置玩家信息
//		if(Config.isShare() && player != null){
//			player = SharePlayerMgr.getInstance().getPlayer(player);
//		}
        return player;
    }

    /**
     * 更新玩家信息 从数据库获取指定玩家数据
     *
     * @param playerID 玩家ID
     * @return
     */
    public boolean updatePlayerMgr(long playerID) {
//		PlayerBO playerBO = BM.getBM(PlayerBO.class).findOne("id", playerID);
        PlayerBO playerBO = playerBOService.findOne(playerID, null);
        if (playerBO == null) {
            return false;
        }
        Player player = new Player(playerBO);
        lock();
        this.allPlayerMap.put(playerID, player);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(player, "playerBO");
        }
        unlock();
        return true;
    }

    /**
     * 更新玩家信息
     *
     * @param playerID
     * @return
     */
    public boolean updatePlayer(long playerID) {
        Player player = this.getPlayer(playerID);
        if (player == null) {
            return false;
        }
        //PlayerBO playerBO = BM.getBM(PlayerBO.class).findOne("id", playerID);
        PlayerBO playerBO = playerBOService.findOne(playerID, null);
        if (playerBO == null) {
            return false;
        }
        lock();
        player.updatePlayer(playerBO);
        //更新共享玩家
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().updateField(player, "playerBO");
        }
        unlock();
        return true;
    }

    // 判断玩家是否存在
    public boolean havePlayerByAccountID(long accountID) {
        if (!allAccountIDPlayerIDMap.containsKey(accountID)) {
            return false;
        }
        long playerID = allAccountIDPlayerIDMap.get(accountID);

        Player player = allPlayerMap.get(playerID);
        if (player == null) {
            CommLogD.error("havePlayerByAccountID({},{}) not find player", accountID, playerID);
            return false;
        }
        return true;
    }

    /**
     * 通过账号ID获取玩家对象
     *
     * @param accountID
     * @return
     */
    public Player getPlayerByAccountID(long accountID) {

        if (!allAccountIDPlayerIDMap.containsKey(accountID)) {
            CommLogD.error("getPlayerByAccountID({}) not find", accountID);
            return null;
        }

        long playerID = allAccountIDPlayerIDMap.get(accountID);
        Player player = allPlayerMap.get(playerID);
        if (player == null) {
            CommLogD.error("getPlayerByAccountID({},{}) not find player", playerID, accountID);
        }
        //共享获取玩家信息
//		if(Config.isShare()){
//			player = SharePlayerMgr.getInstance().getPlayer(player);
//		}
        return player;

    }

    public long accountIDGetPid(long accountID) {
        long playerID = allAccountIDPlayerIDMap.get(accountID);
        return playerID;
    }

    /**
     * 检查账号ID是否存在
     *
     * @return
     */
    public boolean checkAccountIDExist(long accountID) {
        return allAccountIDPlayerIDMap.containsKey(accountID);
    }

    /**
     * 通过名字来检索
     *
     * @param playerName 玩家名称
     * @return
     */
    public boolean havePlayerByName(String playerName) {
        return allNamePlayerIDMap.containsKey(playerName);
    }

    /**
     * 所有以存储玩家
     *
     * @return
     */
    public List<Player> getAllPlayers() {
        return Lists.newArrayList(allPlayerMap.values());
    }

    /**
     * 所有在线玩家集合
     *
     * @return
     */
    public Collection<Long> getPlayerIDList() {
        return Lists.newArrayList(allPlayerMap.keySet());
    }

    /**
     * 所有在线玩家集合
     *
     * @return
     */
    public Collection<Player> getOnlinePlayers() {
        List<Player> players = Lists.newArrayList(allOnLinePlayerMap.values());
        return players;
    }

    /**
     * 所有在线玩家集合
     *
     * @return
     */
    public Collection<Long> getOnlinePlayerIDList() {
        return Lists.newArrayList(allOnLinePlayerMap.keySet());
    }

    /**
     * 获取在线玩家人数
     *
     * @return
     */
    public int getOnlinePlayerSize() {
        //更新共享玩家
        if (Config.isShare()) {
            return SharePlayerMgr.getInstance().onlineSharePlayerSize();
        } else {
            return this.onlinePlayerSize.get();
        }
    }

    /**
     * 获取在线玩家人数
     *
     * @return
     */
    public int getOnlinePlayerSizeReal() {
        //更新共享玩家
        return this.onlinePlayerSize.get();
    }

    /**
     * 获取在线玩家人数
     *
     * @return
     */
    public int getOnlinePlayerSizeReal2() {
        return this.allOnLinePlayerMap.size();
    }

    /**
     * 校验在线人数
     */
    public void checkOnlinePlayerSize() {
        this.onlinePlayerSize.set(this.allOnLinePlayerMap.size());
    }

    /**
     * 创建玩家对象
     *
     * @param newBO
     * @return
     */
    public Player createPlayer(PlayerBO newBO) {
        Player ret = null;
        try {
            lock();
            if (newBO.getId() <= 0L) {
                newBO.setId(playerBOService.nextRandomPlayerId());
            }
            boolean suc = playerBOService.save(newBO) > -1;
            if (suc) {
                ret = new Player(newBO);
                regPlayer(ret, true);
            }
        } catch (Exception e) {
            CommLogD.error("数据库中已经存在名为{}的玩家", newBO.getName());
        } finally {
            unlock();
        }
        return ret;
    }

    /**
     * 记录等待创建角色的账号ID连接
     */
    public void addWaitCreateAccountID(long accountID, ClientSession session) {
        allWaitCreateMap.put(accountID, session);
    }

    /**
     * 删除等待创建角色的账号ID连接
     */
    public void deleteWaitCreateAccountID(long accountID) {
        allWaitCreateMap.remove(accountID);
    }

    /**
     * 获取等待创建角色的账号ID连接session
     */
    public ClientSession getWaitCreateSessionByAccountID(long accountID) {
        return allWaitCreateMap.get(accountID);
    }

    /**
     * session与Player建立连接,这个锁必须是独立的,只保证链接对接的顺序同步
     *
     * @param session
     * @param player
     * @param isUUID  T:通过UUID登录，F:正常账号登录
     * @return
     */
    public synchronized boolean connectPlayer(ClientSession session, Player player) {
        ClientSession oldSession = player.getClientSession();
        // 重复enter
        if (oldSession == session) {
            return true;
        }

        // ================= 释放旧的 ======================
        if (oldSession != null) {
            // 旧的连接，被替换
            oldSession.losePlayer();
        }

        Player oldPlayer = session.getPlayer();
        if (oldPlayer != null) {
            // 旧的账号， 失去连接
            oldPlayer.loseSession();
        }

        // ================= 对接新的 ======================
        session.bindPlayer(player);
        player.bindSession(session);
        //设置共享玩家在线
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().setSharePlayerToOnline(player.getPid());
        }
        return true;
    }


    /**
     * session与Player建立连接,这个锁必须是独立的,只保证链接对接的顺序同步
     *
     * @param session
     * @param player
     * @param isUUID  T:通过UUID登录，F:正常账号登录
     * @return
     */
    public synchronized boolean uuidConnectPlayer(ClientSession session, Player player) {
        ClientSession oldSession = player.getClientSession();
        // 重复enter
        if (oldSession == session) {
            return true;
        }

        // ================= 释放旧的 ======================
        if (oldSession != null) {
            // 旧的连接，被替换
            oldSession.losePlayer();
        }

        Player oldPlayer = session.getPlayer();
        if (oldPlayer != null) {
            // 旧的账号， 失去连接
            oldPlayer.uuidLoseSession();
        }

        // ================= 对接新的 ======================
        session.bindPlayer(player);
        player.uuidBindSession(session);
        return true;
    }

    /**
     * 改名更新缓存
     *
     * @param playerID
     * @param oldName
     * @param newName
     */
    public void onPlayerRename(long playerID, String oldName, String newName) {
        lock();
        Player player = getPlayer(playerID);
        if (player != null) {
            this.allNamePlayerIDMap.remove(oldName);
            this.allNamePlayerIDMap.put(newName, playerID);
        }
        unlock();
    }

    public ErrorCode createPlayer(ClientSession session, int serverID, String headImageUrl, int tourist) {
        Player player = null;
        this.lock();
        try {
            long result = ContainerMgr.get().getComponent(PlayerBOService.class).createPlayer(session, serverID, headImageUrl, 0, 0L,
                    0, tourist);
            if (result > -1) {
                PlayerBO playerBO = ContainerMgr.get().getComponent(PlayerBOService.class).findOne(result, null);
                player = new Player(playerBO);
                regPlayer(player, true);
            }
            if (player == null) {
                return ErrorCode.NotAllow;
            }
        } catch (Exception e) {
            CommLogD.error("创建角色失败出现异常", e);
            return ErrorCode.NotAllow;
        } finally {
            this.unlock();
        }
        this.connectPlayer(session, player);

        return ErrorCode.Success;
    }

    /**
     * 创建玩家Player
     *
     * @param session
     * @param accountID
     * @param serverID
     * @param name
     * @param headImageUrl
     * @param sex
     * @param familyID
     * @param real_referer
     * @param tourist
     * @return
     */
    public ErrorCode createPlayer(ClientSession session, long accountID, int serverID, String name, String headImageUrl,
                                  int sex, long familyID, int real_referer, int tourist) {
        Player player = null;
        this.lock();
        try {
            PlayerBO bo = new PlayerBO();
            bo.setAccountID(accountID);
            bo.setSid(serverID);
            bo.setName(name);
            bo.setSex(sex);
            bo.setCreateTime(CommTime.nowMS());
            bo.setHeadImageUrl(headImageUrl);
            bo.setFamilyID(familyID);
            bo.setRealReferer(real_referer);
            bo.setWxUnionid(session.getWxUnionid());
            bo.setIcon(tourist);
            bo.setPhone(session.getPhone());
            bo.setFastCard(bo.getPhone() > 0L ? 1 : 0);
            initPlayerBO(bo, 0);
            player = this.createPlayer(bo);
            if (player == null) {
                return ErrorCode.NotAllow;
            }
            player.setUUID(session.getToken());
        } catch (Exception e) {
            CommLogD.error("创建角色失败出现异常", e);
            return ErrorCode.NotAllow;
        } finally {
            this.unlock();
        }
        this.connectPlayer(session, player);

        return ErrorCode.Success;
    }

    /**
     * 根据配表初始化
     *
     * @param bo
     * @param initType
     */
    public void initPlayerBO(PlayerBO bo, int initType) {
        bo.setLv(0);
        bo.setVipLevel(0);
        bo.setGmLevel(0);
        bo.setRoomCard(GameConfig.NewPlayerCard());
        bo.setCrystal(GameConfig.NewPlayerCrystal());
        bo.setGold(GameConfig.NewPlayerGold());
    }

    /**
     * 释放玩家-对组建进行卸载
     *
     * @param activeTime
     */
    public void releasPlayer(int activeTime) {
        for (Player player : this.getAllPlayers()) {
            player.releaseFeature(activeTime);
        }
    }

    /**
     * 排行榜使用
     *
     * @param accountID
     * @return
     */
    public long accountIDGetPidRank(long accountID) {
        // 检查是否有accountID
        if (this.allAccountIDPlayerIDMap.containsKey(accountID)) {
            // 获取  accountID 对应的 pid
            return allAccountIDPlayerIDMap.get(accountID);
        } else {
            // 否则 pid == 0;
            return 0L;
        }
    }


    /**
     * 检查手机号是否存在
     *
     * @param phone
     * @return
     */
    public boolean checkExistPhone(long phone) {
        return this.allPlayerMap.values().stream().filter(k -> k.getPlayerBO().getPhone() == phone).findAny().isPresent();
    }

    /**
     * 通过电话获取用户信息
     *
     * @param phone
     * @return
     */
    public Player getPlayerPhone(long phone) {
        Player player = this.allPlayerMap.values().stream().filter(k -> k.getPlayerBO().getPhone() == phone).findAny().orElse(null);
        //共享获取玩家信息
//		if(Config.isShare()){
//			return SharePlayerMgr.getInstance().getPlayer(player);
//		} else {
        return player;
//		}
    }

    /**
     * 设置玩家手机号
     *
     * @param unionid 微信UUID
     * @param phone   玩家手机号
     * @return
     */
    public String setPlayerPhone(String unionid, long phone, long oldPhone) {
        Player player = this.allPlayerMap.values().stream().filter(k -> unionid.equals(k.getPlayerBO().getWx_unionid())).findAny().orElse(null);
        if (null == player) {
            return ZleData_Result.make(ErrorCode.Player_PidError, "Exist_Phone");
        }
        if (oldPhone > 0L) {
            if (player.getPlayerBO().getPhone() != oldPhone) {
                return ZleData_Result.make(ErrorCode.Error_Old_Phone, "ERROR_oldPhone");
            }
        } else {
            if (player.getPlayerBO().getPhone() > 0L) {
                return ZleData_Result.make(ErrorCode.Error_Phone, "Exist_Phone");
            }
        }
        if (player.getPlayerBO().getPhone() == phone) {
            return ZleData_Result.make(ErrorCode.Exist_Phone, "Exist_Phone ==");
        }
        if (player.getPlayerBO().getPhone() <= 0L) {
            player.getFeature(PlayerWallet.class).gainItemFlow(GameConfig.Phone(), ItemFlow.Phone);
        }
        player.getPlayerBO().savePhone(phone);
        return ZleData_Result.make(ErrorCode.Success, "success");
    }

    /**
     * 设置玩家手机号
     *
     * @param phone   玩家手机号
     * @return
     */
    public String setPlayerPhone1(long accountId, long phone) {
        Player player = this.allPlayerMap.values().stream().filter(k -> accountId == k.getPlayerBO().getAccountID()).findAny().orElse(null);
        if (null == player) {
            return ZleData_Result.make(ErrorCode.Player_PidError, "Exist_Phone");
        }
        if (player.getPlayerBO().getPhone() == phone) {
            return ZleData_Result.make(ErrorCode.Exist_Phone, "Exist_Phone ==");
        }
        if (player.getPlayerBO().getPhone() <= 0L) {
            player.getFeature(PlayerWallet.class).gainItemFlow(GameConfig.Phone(), ItemFlow.Phone);
        }
        player.getPlayerBO().savePhone(phone);
        return ZleData_Result.make(ErrorCode.Success, "success");
    }



    /**
     * 检查玩家手机号
     *
     * @param unionid 微信UUID
     * @return
     */
    public String checkPlayerPhone(String unionid) {
        Player player = this.allPlayerMap.values().stream().filter(k -> unionid.equals(k.getPlayerBO().getWx_unionid())).findAny().orElse(null);
        if (null == player) {
            return ZleData_Result.make(ErrorCode.Player_PidError, "Exist_Phone");
        }
        if (player.getPlayerBO().getPhone() <= 0L) {
            return ZleData_Result.make(ErrorCode.Not_Exist_Phone, "0");
        }
        return ZleData_Result.make(ErrorCode.Success, StringUtil.getPhone(String.valueOf(player.getPlayerBO().getPhone())));
    }

    public SData_Result checkForcePhone(Player player) {
        if (player.getPlayerBO().getPhone() > 0L) {
            // 存在电话号
            return SData_Result.make(ErrorCode.Exist_Phone, String.valueOf(player.getPlayerBO().getPhone()));
        }
        if (player.getPlayerBO().getSendClubReward() <= 0L && (player.getFamiliID() == Family.DefaultFamilyID || player.getFamiliID() <= 0)) {
            // 没必要强制绑定
            return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
        }
        SData_Result result = player.getFeature(PlayerFamily.class).checkFamilyOwnerPhone();
        if (ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        if (ClubMgr.getInstance().getClubMemberMgr().checkClubMemberForcePhone(player.getPid())) {
            return SData_Result.make(ErrorCode.Success);
        }
        return SData_Result.make(ErrorCode.NotAllow, "NotAllow");
    }

    /**
     * 获取玩家的基本信息
     *
     * @param req
     * @return
     */
    public SData_Result getPlayerInfo(CPlayer_GetPlayerInfo req) {
        Player player = this.getPlayer(req.pid);
        if (Objects.isNull(player)) {
            // 没必要强制绑定
            return SData_Result.make(ErrorCode.NotAllow, "Player is null");
        }
        return SData_Result.make(ErrorCode.Success, player.getShortPlayer());
    }

    public void clearInviteInfo() {
        if (Config.isShare()) {
            SharePlayerMgr.getInstance().clearInviteInfo();
        } else {
            for (Player con : getAllPlayers()) {
                con.setInviteFlag(true);
            }
        }
    }
}
