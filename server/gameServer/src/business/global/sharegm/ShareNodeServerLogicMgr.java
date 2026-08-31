package business.global.sharegm;

import business.global.shareroom.LocalRoomMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.rocketmq.bo.MqClubMemberAllUpdateNotifyBo;
import business.rocketmq.bo.MqGameStartToHallNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.LocalPlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;

import java.util.Map;

/**
 * @author xsj
 * @date 2020/9/3 10:22
 * @description 节点启停逻辑处理
 */
public class ShareNodeServerLogicMgr {
    private static ShareNodeServerLogicMgr instance = new ShareNodeServerLogicMgr();

    public static ShareNodeServerLogicMgr getInstance() {
        return instance;
    }

    /**
     * 服务初始化逻辑
     */
    public void initLogic() {
        notifyHall();
    }

    /**
     * 服务销毁逻辑
     */
    public void destroy() {
        try {
            Map<Long, SharePlayer> shareOnlinePlayers = SharePlayerMgr.getInstance().onlineSharePlayers();
            //清理房间玩家
            SharePlayerMgr.getInstance().cleanNodePlayer(shareOnlinePlayers);
            if (Config.isShareLocal()) {
                Map<String, ShareRoom> shareRooms = LocalRoomMgr.getInstance().allShareRooms();
                Map<Long, SharePlayer> sharePlayers = LocalPlayerMgr.getInstance().allSharePlayers();
                //清除房间信息
                ShareRoomMgr.getInstance().cleanNodeRoom(shareRooms, sharePlayers);
            } else {
                Map<String, ShareRoom> shareRooms = ShareRoomMgr.getInstance().allShareRooms();
                Map<Long, SharePlayer> sharePlayers = SharePlayerMgr.getInstance().allSharePlayers();
                //清除房间信息
                ShareRoomMgr.getInstance().cleanNodeRoom(shareRooms, sharePlayers);
            }
            //本节点关闭通知其他大厅节点更新
            notifyOtherHallClubMemberAllUpdate();
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            CommLogD.error(e.getMessage(), e);
        }
    }

    /**
     * 通知大厅
     */
    private void notifyHall() {
        MqProducerMgr.get().send(MqTopic.GAME_START_TO_HALL_NOTIFY, new MqGameStartToHallNotifyBo(Config.nodeName()));
    }

    /**
     * 本节点关闭通知其他大厅节点更新
     */
    public void notifyOtherHallClubMemberAllUpdate() {
        if (Config.nodeName().startsWith("hall")) {
            MqProducerMgr.get().send(MqTopic.CLUB_MEMBER_ALL_UPDATE, new MqClubMemberAllUpdateNotifyBo(Config.nodeName()));
        }
    }
}
