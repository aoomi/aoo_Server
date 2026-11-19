package core.timing;

import business.global.GM.MaintainGameMgr;
import business.global.GM.MaintainServerMgr;
import business.global.room.ContinueRoomInfoMgr;
import business.global.room.RoomRecordMgr;
import business.global.sharegm.ShareNodeServerMgr;
import business.global.union.UnionMgr;
import business.player.PlayerMgr;
import business.player.feature.PlayerFriendsHelpUnfoldRedPack;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.annotation.CronTask;
import com.ddm.server.annotation.Task;
import com.ddm.server.common.Config;
import com.ddm.server.protocol.v2.ProtocolMigrationTelemetry;
import core.db.other.DBFlowMgr;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.other.PromotionActiveReportFormEvent;
import core.dispatch.event.other.SportsPointErrorCheckEvent;
import core.dispatch.event.promotion.PromotionLevelCountEvent;
import core.dispatch.event.promotion.PromotionLevelCountZhongZhiEvent;
import core.dispatch.event.promotion.PromotionPointCountEvent;
import core.dispatch.event.union.SportsPointChangeZhongZhiCountEvent;
import core.dispatch.event.union.UnionExamineAutoEvent;
import core.logger.flow.FlowLogger;

/**
 * 定时处理器
 */
@Task
public class TimingProcessor {

    /**
     * 每天23时59分59秒
     */
    @CronTask("59 59 23 * * ? *")
    public void everyDay23Hour() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            DispatcherComponent.getInstance().publish(new PromotionActiveReportFormEvent(false));
        }
    }


    /**
     * 每天0时0分0秒
     */
    @CronTask("0 0 0 * * ? *")
    public void everyDay0Hour() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
//            PlayBackMgr.getInstance().clear();
            DispatcherComponent.getInstance().publish(new PromotionActiveReportFormEvent(true));
            //每日一表创建
            DBFlowMgr.getInstance().clubLevelRoomLog();
            DBFlowMgr.getInstance().roomPromotionPointLogLog();
            DBFlowMgr.getInstance().roomSportsPointChangeZhongZhiLog();
        }

    }

    /**
     * 每15秒执行
     */
    @CronTask("0/15 * * * * ?")
    public void every15Sec() {
        // 心跳活动
        PlayerMgr.getInstance().getOnlinePlayers().parallelStream().forEach(k -> k.heartBeat());
    }

    /**
     * 每15分钟执行
     */
    @CronTask("0 0/15 * * * ?")
    public void every15Minu() {
        // Independent protocol observation heartbeat. SET NX keeps this safe across hall nodes.
        ProtocolMigrationTelemetry.observeToday();
        // 记录每15分钟的在线人数
        FlowLogger.playerOnLineChargeLog(PlayerMgr.getInstance().getOnlinePlayerSize());
    }

    /**
     * 每2分钟执行
     */
    @CronTask("0 0/2 * * * ?")
    public void every2Minu() {
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            // 删除超时一个月的游戏房间
            RoomRecordMgr.getInstance().asyncDelGameRoom();
        }
    }

    /**
     * 每5分钟执行
     */
    @CronTask("0 0/5 * * * ?")
    public void every5Minu() {
        // 每5分钟检查维护是否关闭
        MaintainServerMgr.getInstance().checkCompleteServer();
        // 检查红包任务
        PlayerMgr.getInstance().getOnlinePlayers().parallelStream().forEach(k -> k.getFeature(PlayerFriendsHelpUnfoldRedPack.class).checkRedPackTaskShow());
    }

    /**
     * 每1小时执行
     */
    @CronTask("0 0 * * * ?")
    public void every1Hour() {
        // 登记玩家的在线时间
        PlayerMgr.getInstance().getOnlinePlayers().parallelStream().forEach(k -> k.hourTimePushProto());
        //删除房卡房继续房间过期的信息
        ContinueRoomInfoMgr.getInstance().continueRoomInfoOutTimeRemove();
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            // 比赛分总分检查
            DispatcherComponent.getInstance().publish(new SportsPointErrorCheckEvent());
        }

    }


    /**
     * 每天凌晨1点执行
     */
    @CronTask("0 0 1 * * ?")
    public void everyDay1Hour() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            // 统计昨天推广员数据
            DispatcherComponent.getInstance().publish(new PromotionLevelCountEvent());
            // 统计昨天推广员房间分成数据
            DispatcherComponent.getInstance().publish(new PromotionPointCountEvent());
            // 统计中至数据表
            DispatcherComponent.getInstance().publish(new SportsPointChangeZhongZhiCountEvent());
        }
    }

    /**
     * 每天凌晨6点执行
     */
    @CronTask("0 0 6 * * ?")
    public void everyDay6Hour() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            //中至每日一表的创建
            DBFlowMgr.getInstance().clubLevelRoomLogZhongZhi();
            // 比赛分总分检查
            DispatcherComponent.getInstance().publish(new SportsPointErrorCheckEvent());
            // 执行赛事比赛排名
            UnionMgr.getInstance().execUnionMatchRanking();

        }
    }

    /**
     * 每天凌晨6点10分执行
     * 中至的统计数据
     */
    @CronTask("0 10 6 * * ?")
    public void everyDay6Hour10Min() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            // 中至模式统计昨天推广员数据
            DispatcherComponent.getInstance().publish(new PromotionLevelCountZhongZhiEvent());

        }
    }

    /**
     * 每天凌晨6点半执行
     */
    @CronTask("0 30 6 * * ?")
    public void everyDay6HalfHour() {
        //大厅才需要执行
        if (ShareNodeServerMgr.getInstance().checkIsMasterHall()) {
            //联盟自动审核功能
            DispatcherComponent.getInstance().publish(new UnionExamineAutoEvent());
            //设置每个赛事皮肤修改状态
            UnionMgr.getInstance().changeSkinType();
            //重置每个玩家身上的邀请状态
            PlayerMgr.getInstance().clearInviteInfo();

        }

    }

    /**
     * 每1分钟执行
     */
    @CronTask("0 0/1 * * * ?")
    public void every1Minu() {
        if (!MaintainServerMgr.getInstance().isMaintainServerProgram()) {
            //发送节点服务器表示节点还存在
            ShareNodeServerMgr.getInstance().addOrUpdate();
            //删除遗留redis在线玩家
            SharePlayerMgr.getInstance().onlineSharePlayers().values().stream()
                    .filter(k -> SharePlayerMgr.getInstance().checkCurNodePlayer(k.getCurShareNode()) && PlayerMgr.getInstance().getOnlinePlayerByPid(k.getPlayerBO().getId()) == null)
                    .forEach(k -> SharePlayerMgr.getInstance().removeOnlineSharePlayer(k.getPlayerBO().getId()));
        }
        if (!Config.nodeName().startsWith("hall")) {
            //通知节点游戏维护消息
            MaintainGameMgr.getInstance().notifyThisGameAll();
        }
    }


}
