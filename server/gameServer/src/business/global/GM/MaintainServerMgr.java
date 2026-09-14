package business.global.GM;

import BaseCommon.CommLog;
import BaseThread.BaseMutexManager;
import business.global.room.RoomMgr;
import business.global.sharegm.ShareNodeServerLogicMgr;
import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.task.ScheduledExecutorServiceMgr;
import com.ddm.server.common.task.TaskMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.http.server.MGHttpServer;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.MaintainServerBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.MaintainServerBOService;
import core.ioc.Constant;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import core.network.client2game.ClientAcceptor;
import core.network.http.proto.SData_Result;
import core.server.OpenSeverTime;
import lombok.Data;
import org.quartz.SchedulerException;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 维护服务端管理器
 *
 * @author Huaxing
 */
@Data
public class MaintainServerMgr {
    private static MaintainServerMgr instance = new MaintainServerMgr();
    private final BaseMutexManager _lock = new BaseMutexManager();

    public static MaintainServerMgr getInstance() {
        return instance;
    }

    private long maintainStartTime = 0;
    // 逻辑层面检查维护状态 T:维护中，F:维护完毕
    private boolean isMaintainServer = false;

    // 程序层面维护 T:维护中，F:维护完毕
    private boolean isMaintainServerProgram = false;
    // 维护结束时间
    private int maintainTime = 0;
    private MaintainServerBO maintainServerBO = null;

    // 紧急维护中 T:维护中，F:维护完毕
    private boolean urgentMaintainServer = false;

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * 初始化服务
     */
    public void init() {
        try {
            lock();
            // 获取数据库中的数据，进行维护时间的检测
            this.checkMaintainBO();
            // 虚拟机关闭时的调用钩子
            Runtime.getRuntime().addShutdownHook(new MainitainThread());
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        } finally {
            unlock();
        }
    }

    /**
     * 关闭虚拟机
     */
    public void shutdownHook() {
        //如果不是紧急维护才调用钩子方法
        if (!urgentMaintainServer) {
            CommLog.info("更新停止节点");
            //设置状态维护中
            this.setMaintainServer(true);
            //设置程序维护中
            this.setMaintainServerProgram(true);
            //设置维护开始时间
            this.setMaintainStartTime(System.currentTimeMillis());
            // 更新停止节点
            ShareNodeServerMgr.getInstance().stopNodeServer();
            CommLog.info("关闭房间");
            // 关闭房间
            this.stopServerWaitStopAllRoom();
            //退出玩家
            if (Config.isKickPlayer()) {
                CommLog.info("退出玩家");
                this.stopServerPlayerOut();
            }
            CommLog.info("共享节点销毁逻辑");
            // 共享节点销毁逻辑
            ShareNodeServerLogicMgr.getInstance().destroy();
            // 通知节点退出
            MqConsumerTopicFactory.getInstance().stopCurConnect();
//        // 关闭MQ接收
//        CommLog.info("关闭MQ接收");
//        MqConsumerMgr.get().shutdown();
            CommLog.info("停止托管线程");
            // 停止托管线程
            ScheduledExecutorServiceMgr.getInstance().shutdown();
            CommLog.info("关闭quartz");
            // 关闭quartz
            this.shutdownAllTaskMgr();
            CommLog.info("关闭通信线程");
            // 关闭通信线程（mina）
            ClientAcceptor.getInstance().close();
            CommLog.info("关闭netty");
            // 关闭netty
            ClientAcceptor.getInstance().shutdown();
            CommLog.info("1秒后关闭httpServer");
            // 1秒后关闭httpServer
            MGHttpServer.getInstance().stop();
            CommLog.info("强制关闭java");
            // 强制关闭java 进程
            this.killJava(gameServerMaintainLog());
        }
    }

    /**
     * 紧急维护
     */
    public void urgentMaintainServer() {
        //紧急维护中
        this.setUrgentMaintainServer(true);
        //设置状态维护中
        this.setMaintainServer(true);
        //设置程序维护中
        this.setMaintainServerProgram(true);
        //设置维护开始时间
        this.setMaintainStartTime(System.currentTimeMillis());
        // 更新停止节点
        ShareNodeServerMgr.getInstance().stopNodeServer();
        CommLog.info("关闭房间");
        // 关闭房间
        this.restartServer();
        //立刻踢出已经不再房间的玩家
        this.stopServerKickPlayerOut(PlayerMgr.getInstance().getOnlinePlayers());
        CommLog.info("共享节点销毁逻辑");
        // 通知节点退出
        MqConsumerTopicFactory.getInstance().stopCurConnect();
        // 共享节点销毁逻辑
        ShareNodeServerLogicMgr.getInstance().destroy();
        CommLog.info("停止托管线程");
        // 停止托管线程
        ScheduledExecutorServiceMgr.getInstance().shutdown();
        CommLog.info("关闭quartz");
        // 关闭quartz
        this.shutdownAllTaskMgr();
        CommLog.info("关闭通信线程");
        // 关闭通信线程（mina）
        ClientAcceptor.getInstance().close();
        CommLog.info("关闭netty");
        // 关闭netty
        ClientAcceptor.getInstance().shutdown();
        CommLog.info("1秒后关闭httpServer");
        // 1秒后关闭httpServer
        MGHttpServer.getInstance().stop();
        CommLog.info("强制关闭java");
        // 强制关闭java 进程
        this.killJava(gameServerMaintainLog());
    }

    /**
     * 踢出节点
     */
    public void kickOutServer() {
        // 关闭房间
        this.restartServer();
        //立刻踢出已经不再房间的玩家
        this.stopServerKickPlayerOut(PlayerMgr.getInstance().getOnlinePlayers());
    }

    /**
     * 踢出游戏
     */
    public void kickOutGame(Integer gameTypeId) {
        // 解散所有的房间，并且返回房卡
        List<Player> listPlayer = RoomMgr.getInstance().cleanAllRoomByGameType(gameTypeId);
        //立刻踢出已经不再房间的玩家
        this.stopServerKickPlayerOut(listPlayer);

    }

    /**
     * 维护日志
     *
     * @return
     */
    public int gameServerMaintainLog() {
        // 开始时间
        long start = OpenSeverTime.getInstance().getStartServerTime();
        // 结束时间
        long end = CommTime.nowMS();
        // 间隔时间
        int sec = CommTime.SecondsBetween(start, end);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int pid = Integer.parseInt(name.split("@")[0]);
        FlowLogger.gameServerMaintainLog(start, end, sec, Integer.getInteger("Server.HttpServer", 9888), Integer.getInteger("GameServer.ClientPort"), name, pid);
        CommLog.info("Pid:{}", pid);
        return pid;
    }

    private void killJava(int pid) {
        try {
            boolean stopped = ProcessHandle.of(pid)
                    .filter(ProcessHandle::isAlive)
                    .map(ProcessHandle::destroyForcibly)
                    .orElse(false);
            CommLog.info("killJava pid:{},stopped:{}", pid, stopped);
        } catch (Exception e) {
            CommLog.error("killJava error:{}", e.getMessage(), e);
        }
    }

    /**
     * 重启服务器
     */
    private void restartServer() {
        try {
            CommLog.info("MaintainServerMgr restartServer");
            // 解散所有的房间，并且返回房卡
            RoomMgr.getInstance().cleanAllRoom();
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
    }

    /**
     * 检查和停止房间
     */
    private void stopServerWaitStopAllRoom() {
        CommLog.info("开始等待关闭房间");
        while (!Thread.currentThread().isInterrupted()) {
            // 检查是否所有房间都完成了
            boolean finish = RoomMgr.getInstance().checkAllRoomFinish();
            long maintainTime = System.currentTimeMillis() - this.getMaintainStartTime();
            if (!finish && maintainTime <= TimeUnit.MINUTES.toMillis(Config.restartServerWaitTime())) {
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    CommLog.error(e.getMessage(), e);
                    break;
                }
            } else {
                //房间超过时间没有完成强行清退
                restartServer();
                break;
            }
        }

    }

    /**
     * 退出玩家
     */
    private void stopServerPlayerOut() {
        if (Config.isWaitPlayerOut()) {
            //等待玩家自己退出
            stopServerWaitPlayerOut();
        } else {
            //立刻踢出已经不再房间的玩家
            stopServerKickPlayerOut(PlayerMgr.getInstance().getOnlinePlayers());
        }
    }

    /**
     * 立刻踢出玩家
     */
    private void stopServerKickPlayerOut(Collection<Player> playerList) {
        CommLog.info("开始通知玩家退出");
        CommLog.info("节点玩家数量[{}]", playerList.size());
        for (Player player : playerList) {
            if (player != null && player.getSession() != null) {
                try {
                    player.getSession().kickOutPlayer();
                } catch (Exception e) {
                    CommLogD.error(e.getMessage(), e);
                }
            }
        }
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CommLog.error(e.getMessage(), e);
        }
        CommLog.info("通知玩家退出完成");
        //有的玩家没有退出服务端踢出
        stopServerLosePlayer(playerList);
    }

    /**
     * 立刻踢出玩家
     */
    private void stopServerLosePlayer(Collection<Player> playerList) {
        CommLog.info("服务端主动踢出玩家");
        CommLog.info("节点玩家数量[{}]", playerList.size());
        for (Player player : playerList) {
            if (player != null && player.getSession() != null) {
                try {
                    player.getSession().losePlayer();
                    PlayerMgr.getInstance().unregOnlinePlayer(player);
                } catch (Exception e) {
                    CommLogD.error(e.getMessage(), e);
                }
            }
        }
        CommLog.info("服务端主动玩家踢出完成");
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CommLog.error(e.getMessage(), e);
        }
    }

    /**
     * 等待玩家自己退出
     */
    private void stopServerWaitPlayerOut() {
        CommLog.info("开始等待玩家退出");
        while (!Thread.currentThread().isInterrupted()) {
            // 检查所有玩家是否退出
            int onlineNumber = PlayerMgr.getInstance().getOnlinePlayerSizeReal2();
            CommLog.info("节点玩家数量[{}]", onlineNumber);
            long maintainTime = System.currentTimeMillis() - this.getMaintainStartTime();
            if (onlineNumber > 0 && maintainTime <= TimeUnit.MINUTES.toMillis(Config.restartServerWaitTime())) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    CommLog.error(e.getMessage(), e);
                    break;
                }
            } else {
                break;
            }
        }

    }

    /**
     * 关闭quartz
     */
    private void shutdownAllTaskMgr() {
        try {
            TaskMgr.get().shutdownAll();
        } catch (SchedulerException e) {
            CommLog.error("restartServer error:{}", e.getMessage(), e);
        }

    }

    /**
     * 设置维护状态
     *
     * @param maintainServer
     */
    public void setMaintainServer(boolean maintainServer) {
        isMaintainServer = maintainServer;
        CommLog.info("MaintainServerMgr setMaintainServer:{}", maintainServer);
    }

    public boolean isMaintainServer() {
        return isMaintainServer;
    }

    /**
     * 检查维护时间是否完毕
     */
    public void checkCompleteServer() {
        // 获取数据库中的数据，进行维护时间的检测
        this.checkMaintainBO();
    }


    /**
     * 获取数据库中的数据，进行维护时间的检测
     */
    private void checkMaintainBO() {
        if (Objects.isNull(this.getMaintainServerBO())) {
            this.setMaintainServerBO(ContainerMgr.get().getComponent(MaintainServerBOService.class).findOne(Restrictions.eq("serverId", Constant.serverIid), null));
        }
        //检查是否在维护时间内
        if (Objects.nonNull(this.getMaintainServerBO()) && CommTime.checkTimeIntervale(this.getMaintainServerBO().getStartTime(), this.getMaintainServerBO().getEndTime())) {
            this.setMaintainTime(this.getMaintainServerBO().getEndTime());
            //标记为 T: 维护中
            MqConsumerTopicFactory.getInstance().stopCurConnect();
            this.setMaintainServer(true);
        } else {
            //标记为 F: 维护完毕
            this.setMaintainServer(false);
        }

    }


    /**
     * 检查服务端重启时间
     */
    public boolean checkRestartServerTime() {
        //检查服务端重启时间
        if (Objects.nonNull(this.getMaintainServerBO()) && CommTime.checkTimeIntervale(this.getMaintainServerBO().getStartTime(), this.getMaintainServerBO().getEndTime())) {
            this.setMaintainTime(this.getMaintainServerBO().getEndTime());
//            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
//            restartServer();
            this.setMaintainServer(true);
            CommLogD.error("MaintainServerBO checkTimeIntervale True StartTime :{},EndTime :{}", this.getMaintainServerBO().getStartTime(), this.getMaintainServerBO().getEndTime());
            return true;
        } else {
            //标记为 F: 维护完毕
            this.setMaintainServer(false);
            CommLogD.error("MaintainServerBO checkTimeIntervale false StartTime :{},EndTime :{}", this.getMaintainServerBO().getStartTime(), this.getMaintainServerBO().getEndTime());
            return true;
        }
    }

    /**
     * 检查是否维护中
     * 如果维护中，只有Gm 用户可以进入游戏，进行游戏测试。
     *
     * @param request 请求
     * @param player  玩家
     * @return
     */
    public boolean checkUnderMaintenance(WebSocketRequest request, Player player) {
        SData_Result result = checkUnderMaintenance(player);
        if (ErrorCode.Success.equals(result.getCode())) {
            return true;
        } else {
            request.error(result.getCode(), result.getMsg());
            return false;
        }
    }


    /**
     * 检查是否维护中
     * 如果维护中，只有Gm 用户可以进入游戏，进行游戏测试。
     *
     * @param player 玩家
     * @return
     */
    public SData_Result checkUnderMaintenance(Player player) {
        // 检查是否处于维护中
        if (!this.isMaintainServer) {
            return SData_Result.make(ErrorCode.Success);
        }
        //检查玩家是否有权限进入游戏
        if (Objects.isNull(player) || !player.isGmLevel()) {
            return SData_Result.make(ErrorCode.Server_Maintain, String.valueOf(this.getMaintainTime()));
        }
        return SData_Result.make(ErrorCode.Success);
    }


    /**
     * 设置维护时间
     * 后台设置
     *
     * @param startTime
     * @param endTime
     */
    public void setMaintainServer(int startTime, int endTime) {
        // 设置维护时间
        if (Objects.isNull(this.getMaintainServerBO())) {
            this.setMaintainServerBO(new MaintainServerBO());
        }
        this.getMaintainServerBO().setEndTime(endTime);
        this.getMaintainServerBO().setStartTime(startTime);
        this.getMaintainServerBO().setServerId(Constant.serverIid);
        this.getMaintainServerBO().getBaseService().saveOrUpDate(getMaintainServerBO());
    }

    /**
     * 获取维护时间
     * 后台获取
     *
     * @return
     */
    public MaintainServerBO getMaintainServer() {
        if (Objects.isNull(getMaintainServerBO())) {
            return new MaintainServerBO();
        } else {
            return this.getMaintainServerBO();
        }
    }


}
