/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.network.client2game.handler;

import business.player.Player;
import cenum.ExceptionTypeEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.logger.flow.FlowLogger;
import core.network.client2game.ClientSession;
import jsproto.c2s.cclass.PlayerRequestRecordInfo;

import java.io.IOException;

public abstract class PlayerHandler extends BaseHandler {

    @Override
    public void handle(final WebSocketRequest request, final String message) throws IOException {

        ClientSession session = (ClientSession) request.getSession();
        if (!session.isValid()) {
            return;
        }
        Player player = session.getPlayer();
        if (null == player) {
            request.error(ErrorCode.NotAllow, "玩家未登陆");
            session.close();
            return;
        }
        if (heartBeatHandler(player, request, message)) {
            // 心跳
            return;
        }
        long curTime = CommTime.nowMS();
        // 检查请求出现并发记录
        if (!player.checkAndAddRequestConcurrentRecor(request.getHeader().event, curTime)) {
            request.error(ErrorCode.NotAllow, "request concurrent exception");
            if (player.existExceededConcurrencyLimit(request.getHeader().event)) {
                // 存在同一时间多次并发的玩家
                // TODO 因为客户端心跳请求可能出现并发所有先放弃踢人操作
                // session.losePlayer();
                // 并且记录异常日志中
                CommLogD.error("[PlayerHandler]:[{}] cid:{},gameId:{},sequence:{} request exceededConcurrencyLimit ", request.getHeader().event, player.getPid(), player.getCurrentGameType(), request.getHeader().sequence);
            } else {
                CommLogD.error("[PlayerHandler]:[{}] cid:{},gameId:{},sequence:{} request error", request.getHeader().event, player.getPid(), player.getCurrentGameType(), request.getHeader().sequence);
            }
            return;
        }
        // 分钟内出现超高频率请求次数
        int tempMinuteUltrahighFrequencyCount = player.getMinuteUltrahighFrequencyCount(curTime);
        if (tempMinuteUltrahighFrequencyCount > 0) {
            FlowLogger.playerExceptionLog(player.getPid(), ExceptionTypeEnum.MINUTE_ULTRAHIGH_FREQUENCY_EXCEPTION.getValue(), String.format(ExceptionTypeEnum.MINUTE_ULTRAHIGH_FREQUENCY_EXCEPTION.getContent(), tempMinuteUltrahighFrequencyCount, request.getHeader().event));
        }
        try {
            player.lockIns();
            LegacyReplayGuard.validate(request.getHeader().event, player.getPid(), message);
            LegacyGameRequestGuard.validate(request.getHeader().event, player, message);
            handle(player, request, message);
        } catch (WSException e) {
            CommLogD.warn("[PlayerHandler]:[{}] cid:{} error:{}", request.getHeader().event, player.getPid(),
                    e.getMessage());
            request.error(ErrorCode.NotAllow, e.getMessage());
        } catch (Exception e) {
            CommLogD.error("[PlayerHandler]:[{}] cid:{} error:{}", request.getHeader().event, player.getPid(),
                    e.getMessage(), e);
            request.error(ErrorCode.NotAllow, "服务端处理请求失败");
        } finally {
            player.unlockIns();
            long nowTime = CommTime.nowMS();
            long costSec = nowTime - curTime;
            if (costSec >= 1000) {
                if (player.existOvertimeInterface(nowTime)) {
                    session.losePlayer();
                    // 并且记录异常日志中
                    FlowLogger.playerExceptionLog(player.getPid(), ExceptionTypeEnum.INTERFACE_TIMEOUT_EXCEPTION.getValue(), ExceptionTypeEnum.INTERFACE_TIMEOUT_EXCEPTION.getContent());
                    CommLogD.error("[PlayerHandler]:[{}] cid:{} losePlayer overtime cost:{}", request.getHeader().event,
                            player.getPid(), costSec);
                } else {
                    CommLogD.error("[PlayerHandler]:[{}] cid:{} overtime cost:{}", request.getHeader().event,
                            player.getPid(), costSec);
                }
            }
        }
    }


    /**
     * 心跳操作
     *
     * @param player  玩家信息
     * @param request 请求
     * @param message 内容
     * @return
     */
    private final boolean heartBeatHandler(Player player, WebSocketRequest request, String message) {
        if (PlayerRequestRecordInfo.CHeartBeatHandler.equals(request.getHeader().event)) {
            try {
                handle(player, request, message);
            } catch (WSException e) {
                CommLogD.warn("[HeartBeatHandler]:[{}] cid:{} error:{}", request.getHeader().event, player.getPid(), e.getMessage());
                request.error(ErrorCode.NotAllow, e.getMessage());
            } catch (Exception e) {
                CommLogD.error("[HeartBeatHandler]:[{}] cid:{} error:{}", request.getHeader().event, player.getPid(), e.getMessage(), e);
                request.error(ErrorCode.NotAllow, "服务端处理请求失败");
            }
            return true;
        } else {
            return false;
        }
    }

    public abstract void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException;
}
