package business.global.room;

import BaseCommon.CommLog;
import BaseThread.BaseMutexManager;
import business.global.club.ClubMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.rocketmq.bo.MqPLayerLostConnectNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.ClassType;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.protocol.v2.ProtocolRoomTracker;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.RoomCfgCount;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间管理
 *
 * @author Administrator
 */
public class RoomMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static RoomMgr instance = new RoomMgr();
    }

    // 私有化构造方法
    private RoomMgr() {
    }

    // 获取单例
    public static RoomMgr getInstance() {
        return SingletonHolder.instance;
    }

    private final BaseMutexManager _lock = new BaseMutexManager();


    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    // key：房间ID,value：房间信息
    private Map<Long, AbsBaseRoom> rooms = new ConcurrentHashMap<>();

    /**
     * 获取房间信息
     *
     * @param id 房间ID
     * @return
     */
    public AbsBaseRoom getRoom(long id) {
        return rooms.get(id);
    }

    /**
     * 增加房间信息
     *
     * @param roomId 房间ID
     * @param room   房间信息
     */
    public void roomPut(long roomId, AbsBaseRoom room) {
        this.rooms.put(roomId, room);
    }

    /**
     * 移除房间ID和房间Key
     *
     * @param id
     * @param prizeType
     */
    public void removeRoom(long id, PrizeType prizeType) {
        String key = "";
        AbsBaseRoom room = this.rooms.get(id);
        if (null != room) {
            key = room.getRoomKey();
            room.getRoomTyepImpl().onRoomRemove();
        }
        try {
            lock();
            this.rooms.remove(id);
            LegacyUnifiedRoomBinding.remove(id);
            ProtocolRoomTracker.roomRemoved(id);
            this.remove(key, prizeType);
            if (id > 0 && StringUtils.isEmpty(key)) {
                CommLogD.error("移除房间异常roomId={}, roomKey={}", id, key);
            }
            if (room == null) {
                CommLogD.error("移除房间room is null roomId={}, roomKey={}", id, key);
            }
            //删除共享房间
            if (Config.isShare()) {
                ShareRoomMgr.getInstance().removeShareRoom(key);
            }
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        } finally {
            unlock();
        }

    }

    /**
     * 创建房间管理中移除指定roomKey
     *
     * @param key       房间Key
     * @param prizeType 消耗类型
     */
    private void remove(String key, PrizeType prizeType) {
        if (PrizeType.Gold.equals(prizeType)) {
            //普通的金币消耗类型
            GoldRoomMgr.getInstance().remove(key);
        } else {
            NormalRoomMgr.getInstance().remove(key);
        }
    }

    /**
     * 是否失去连接通知
     *
     * @param player        玩家信息
     * @param isLostConnect T:失去连接,F:建立连接
     */
    public void lostConnect(Player player, boolean isLostConnect) {
        //共享模式mq通知全部节点
        if (Config.isShare()) {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
            if (sharePlayer.getRoomInfo().getRoomId() > 0L) {
                var event=new MqPLayerLostConnectNotifyBo(player.getPid(), sharePlayer.getRoomInfo().getRoomId(), isLostConnect);
                var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.PLAYER_LOST_CONNECT_NOTIFY,
                        "connection:"+player.getPid()+":"+sharePlayer.getRoomInfo().getRoomId()+":"+isLostConnect,event);
                if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED) CommLogD.error("lost-connect MQ unconfirmed pid:{}, roomId:{}, result:{}",player.getPid(),sharePlayer.getRoomInfo().getRoomId(),confirmation);
            }
        } else {
            // 获取房间Id
            long roomId = player.getRoomInfo().getRoomId();
            if (roomId > 0L) {
                // 获取指定房间信息
                AbsBaseRoom room = this.getRoom(roomId);
                if (Objects.nonNull(room)) {
                    room.lostConnect(player.getPid(), isLostConnect);
                    if (!isLostConnect) {
                        // 通知存在相互Ip的玩家
                        room.getRoomPosMgr().notify2ExistSameIp();
                    }
                }
            }
        }
    }

    /**
     * 是否失去连接通知共享情况
     *
     * @param roomId        房间ID
     * @param pid           玩家ID
     * @param isLostConnect T:失去连接,F:建立连接
     */
    public void lostConnectNotifyShare(long roomId, long pid, boolean isLostConnect) {
        if (roomId > 0L) {
            // 获取指定房间信息
            AbsBaseRoom room = this.getRoom(roomId);
            if (Objects.nonNull(room)) {
                room.lostConnect(pid, isLostConnect);
                if (!isLostConnect) {
                    // 通知存在相互Ip的玩家
                    room.getRoomPosMgr().notify2ExistSameIp();
                }
            }
        }
    }


    /**
     * 清除所有房间
     */
    public void cleanAllRoom() {
        CommLogD.info("房间数量{}", this.rooms.size());
        if (this.rooms.size() <= 0) {
            return;
        }
        rooms.values().stream().filter(k -> null != k).forEach(k -> {
            if (null != k) {
                // 系统维护时强制解散
                k.getRoomTyepImpl().doForceDissolve();
            }
        });
    }

    /**
     * 清除指定游戏房间
     */
    public List<Player> cleanAllRoomByGameType(Integer gameTypeId) {
        if (this.rooms.size() <= 0) {
            return null;
        }
        List<Player> listPlayer = new ArrayList<>();
        rooms.values().stream().filter(k -> null != k).forEach(k -> {
            if (null != k && k.getGameRoomBO().getGameType() == gameTypeId) {
                k.getRoomPosMgr().getPosList().stream().filter(p -> null != p).forEach(p -> {
                    listPlayer.add(p.getPlayer());
                });
                // 系统维护时强制解散
                k.getRoomTyepImpl().doForceDissolve();
            }
        });
        return listPlayer;
    }

    /**
     * 检查所有房间如果是初始化阶段就直接解散
     */
    public boolean checkAllRoomFinish() {
        if (this.rooms.size() <= 0) {
            return true;
        }
        CommLog.info("等待关闭房间数量[{}]", this.rooms.size());
        rooms.values().stream().filter(k -> null != k).forEach(k -> {
            if (null != k) {
                if (RoomState.Init.equals(k.getRoomState())) {
                    // 系统维护时强制解散
                    k.getRoomTyepImpl().doForceDissolve();
                }
            }
        });
        return false;
    }


    /**
     * 获取房间配置统计
     *
     * @return
     */
    public RoomCfgCount getRoomCfgCount(RoomTypeEnum roomTypeEnum, ClassType classType, long unionId) {
        // 统计
        final RoomCfgCount roomCfgCount = new RoomCfgCount();
        this.rooms
                .values()
                .parallelStream()
                .filter(k -> RoomState.Playing.equals(k.getRoomState()) && roomTypeEnum.equals(k.getRoomTypeEnum()) && unionId == k.getSpecialRoomId() && (ClassType.NONE.equals(classType) || classType.equals(k.getClassType())))
                .forEach(k -> {
                    roomCfgCount.addRoomCount();
                    roomCfgCount.addPlayerCount(k.getPlayingCount());
                });
        return roomCfgCount;
    }

    /**
     * 检查是否存在指定类型房间
     *
     * @param roomTypeEnum
     * @param specialRoomId
     * @return
     */
    public boolean checkExistSpecialRoom(RoomTypeEnum roomTypeEnum, long specialRoomId) {
        return this.rooms.values().stream().anyMatch(k -> roomTypeEnum.equals(k.getRoomTypeEnum()) && specialRoomId == k.getSpecialRoomId());
    }


    /**
     * 房间邀请好友列表
     *
     * @param clubId  亲友圈Id
     * @param unionId 赛事Id
     * @param pageNum 页数
     * @param query   查询
     * @return
     */
    public SData_Result getRoomInvitationItemList(long clubId, long unionId, int pageNum, String query, int size) {
        if (unionId <= 0L) {
            // 亲友圈
            return ClubMgr.getInstance().getClubMemberMgr().getClubMemberRoomInvitationItemList(clubId, size, pageNum, query);
        }
        // 赛事
        return UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberRoomInvitationItemList(unionId, size, pageNum, query);
    }

    /**
     * 解散还没有开始的房间
     *
     * @param unionId
     * @return
     */
    public boolean cleanAllRoomByUnionId(Long unionId) {
        if (this.rooms.size() <= 0) {
            return true;
        }
        CommLog.info("等待关闭亲友圈联盟[{}]房间数量[{}]", unionId, this.rooms.size());
        rooms.values().stream().filter(k -> null != k && k.getBaseRoomConfigure().getBaseCreateRoom().getUnionId() == unionId).forEach(k -> {
            if (null != k) {
                if (RoomState.Init.equals(k.getRoomState())) {
                    // 系统维护时强制解散
                    k.getRoomTyepImpl().doForceDissolve();
                }
            }
        });
        return false;
    }
}
