package business.shareplayer;

import business.rocketmq.bo.MqPlayerPushProtoBo;
import business.rocketmq.constant.MqTopic;
import cenum.VisitSignEnum;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.StringUtil;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player;
import lombok.Data;

import java.util.Objects;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家对象
 */
@Data
//@MongoDbs({@MongoDb(doc = @Document(collection = "shareOnlinePlayerKey"), indexes = @CompoundIndexes({@CompoundIndex(name = "clubId_1",def = "{'clubId':1}"),@CompoundIndex(name = "unionId_1",def = "{'unionId':1}"),@CompoundIndex(name = "roomId_1",def = "{'roomId':1}"),@CompoundIndex(name = "ip_port",def = "{'nodeIp':1, 'nodePort':1}")})),
//        @MongoDb(doc = @Document(collection = "shareAllPlayerKey"), indexes = @CompoundIndexes({@CompoundIndex(name = "clubId_1",def = "{'clubId':1}"),@CompoundIndex(name = "unionId_1",def = "{'unionId':1}"),@CompoundIndex(name = "roomId_1",def = "{'roomId':1}"),@CompoundIndex(name = "ip_port",def = "{'nodeIp':1, 'nodePort':1}")}))})
public class SharePlayer {
    //更新时间
    private long updateTime;
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
     * 玩家定位信息
     */
    private ShareLocationInfo locationInfo = new ShareLocationInfo();
    /**
     * 玩家房间信息
     */
    private SharePlayerRoomInfo roomInfo = new SharePlayerRoomInfo();
    // 玩家UUID
    private String uUID = "";
    private String ip = "";
    private int hourTime = 0;
    private long lastTime = 0L;
    private int isMobile = 0;
    /**
     * 当天首次登陆
     */
    private boolean isTodayFirstLogin = false;

    /**
     * 虚拟账号不是账号中心的就是虚拟账号
     */
    private boolean isVPlayer;
    /**
     * 玩家基础信息
     */
    private SharePlayerBO playerBO;

    //玩家所在节点
    private ShareNode curShareNode;
    /**
     * 是否允许邀请玩家
     */
    private boolean inviteFlag = true;

    /**
     * mq通知
     *
     * @param msg
     */
    public void pushProtoMq(BaseSendMsg msg) {
        MqProducerMgr.get().send(MqTopic.PLAYER_PUSH_PROTO, new MqPlayerPushProtoBo<>(this.getPlayerBO().getId(), msg, msg.getClass().getName()));
    }

    /**
     * 不在房间中
     *
     * @return
     */
    public boolean notExistRoom() {
        if (!Objects.isNull(this)) {
            if (Objects.isNull(this.getRoomInfo()) || this.getRoomInfo().getRoomId() <= 0) {
                return true;
            }
        }
        // 在房间中
        return !VisitSignEnum.ROOM.equals(this.getSignEnum());
    }

    /**
     * 获取玩家基本信息
     *
     * @return
     */
    public Player.ShortPlayer getShortPlayer() {
        Player.ShortPlayer ret = new Player.ShortPlayer();
        ret.setName(StringUtil.regexMobile(this.playerBO.getName()));
        ret.setPid(this.getPlayerBO().getId());
        ret.setIconUrl(this.playerBO.getHeadImageUrl());
        ret.setAccountID(this.playerBO.getAccountID());
        return ret;
    }

}
