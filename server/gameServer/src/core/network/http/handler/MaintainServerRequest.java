package core.network.http.handler;

import business.global.GM.MaintainServerMgr;
import business.global.sharegm.ShareNodeServer;
import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqDoMaintainServerBo;
import business.rocketmq.bo.MqKickOutGameBo;
import business.rocketmq.bo.MqSetMaintainServerBo;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.db.entity.clarkGame.MaintainServerBO;
import core.network.http.proto.ZleData_Result;

import java.util.List;
import java.util.Objects;

public class MaintainServerRequest {
    private Object Lock = new Object();

    /**
     * 维护服务器
     */
    @RequestMapping(uri = "/maintainServer")
    public void maintainServer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        int maintainServerInt = HttpUtils.getInt(dataJson, "type");
        MqDoMaintainServerBo bo = new MqDoMaintainServerBo(maintainServerInt);
        MqProducerMgr.get().send(MqTopic.DO_MAINTAIN_SERVER, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

    /**
     * 设置维护服务器
     */
    @RequestMapping(uri = "/setMaintainServer")
    public void setMaintainServer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);

        synchronized (Lock) {
            int startTime = HttpUtils.getInt(dataJson, "startTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");

            try {
                //mq通知所有节点
                MqSetMaintainServerBo bo = new MqSetMaintainServerBo(startTime, endTime);
                MqProducerMgr.get().send(MqTopic.SET_MAINTAIN_SERVER, bo);
                response.response(ZleData_Result.make(ErrorCode.Success, "success"));
            } catch (Exception e) {
                CommLogD.error("setMaintainServer error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 获取维护服务器
     */
    @RequestMapping(uri = "/getMaintainServer")
    public void getMaintainServer(HttpRequest request, HttpResponse response) throws Exception {
        response.response(ZleData_Result.make(ErrorCode.Success, MaintainServerMgr.getInstance().getMaintainServer()));
    }

    /**
     * 紧急维护某个节点
     */
    @RequestMapping(uri = "/urgentMaintainServer")
    public void urgentMaintainServer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        MaintainServerBO maintainServerBO = MaintainServerMgr.getInstance().getMaintainServer();
        //检查在维护时间内
        if (Objects.nonNull(maintainServerBO) && CommTime.checkTimeIntervale(maintainServerBO.getStartTime(), maintainServerBO.getEndTime())) {
            //通知节点维护
            String ip = HttpUtils.getString(dataJson, "nodeIp");
            int port = HttpUtils.getInt(dataJson, "nodePort");
            if (Objects.isNull(ip) || port == 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
                return;
            }
            MqUrgentMaintainServerBo bo = new MqUrgentMaintainServerBo(ip, port);
            MqProducerMgr.get().send(MqTopic.URGENT_MAINTAIN_SERVER, bo);
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } else {
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, String.format("不在维护时间")));
            return;
        }

    }

    /**
     * 获取节点信息
     */
    @RequestMapping(uri = "/getShareNode")
    public void getShareNode(HttpRequest request, HttpResponse response) throws Exception {
        List<ShareNodeServer> list = ShareNodeServerMgr.getInstance().allShareNodes();
        response.response(ZleData_Result.make(ErrorCode.Success, list));

    }

    /**
     * 关闭某个节点
     */
    @RequestMapping(uri = "/stopServer")
    public void stopServer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //通知节点维护
        String ip = HttpUtils.getString(dataJson, "nodeIp");
        int port = HttpUtils.getInt(dataJson, "nodePort");
        if (Objects.isNull(ip) || port == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
            return;
        }
        MqUrgentMaintainServerBo bo = new MqUrgentMaintainServerBo(ip, port);
        MqProducerMgr.get().send(MqTopic.STOP_SERVER, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

    /**
     * 踢出节点
     */
    @RequestMapping(uri = "/kickOutServer")
    public void kickOutServer(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //通知节点维护
        String ip = HttpUtils.getString(dataJson, "nodeIp");
        int port = HttpUtils.getInt(dataJson, "nodePort");
        if (Objects.isNull(ip) || port == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
            return;
        }
        MqUrgentMaintainServerBo bo = new MqUrgentMaintainServerBo(ip, port);
        MqProducerMgr.get().send(MqTopic.KICK_OUT_SERVER, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

    /**
     * 踢出游戏
     */
    @RequestMapping(uri = "/kickOutGame")
    public void kickOutGame(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //通知节点维护
        String ip = HttpUtils.getString(dataJson, "nodeIp");
        int port = HttpUtils.getInt(dataJson, "nodePort");
        int gameTypeId = HttpUtils.getInt(dataJson, "gameTypeId");
        if (Objects.isNull(ip) || port == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
            return;
        }
        MqKickOutGameBo bo = new MqKickOutGameBo(ip, port, gameTypeId);
        MqProducerMgr.get().send(MqTopic.KICK_OUT_GAME, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }


    /**
     * 通知服务上线
     */
    @RequestMapping(uri = "/registry2ServerOnline")
    public void registry2ServerOnline(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //通知节点维护
        String ip = HttpUtils.getString(dataJson, "nodeIp");
        int port = HttpUtils.getInt(dataJson, "nodePort");
        int gameTypeId = HttpUtils.getInt(dataJson, "gameTypeId");
        if (Objects.isNull(ip) || port == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
            return;
        }
        MqKickOutGameBo bo = new MqKickOutGameBo(ip, port, gameTypeId);
        MqProducerMgr.get().send(MqTopic.KICK_OUT_GAME, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

    /**
     * 通知服务下线
     */
    @RequestMapping(uri = "/registry2ServerOffline")
    public void registry2ServerOffline(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        //通知节点维护
        String ip = HttpUtils.getString(dataJson, "nodeIp");
        int port = HttpUtils.getInt(dataJson, "nodePort");
        int gameTypeId = HttpUtils.getInt(dataJson, "gameTypeId");
        if (Objects.isNull(ip) || port == 0) {
            response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("ip port error :{%d}", ip)));
            return;
        }
        MqKickOutGameBo bo = new MqKickOutGameBo(ip, port, gameTypeId);
        MqProducerMgr.get().send(MqTopic.KICK_OUT_GAME, bo);
        response.response(ZleData_Result.make(ErrorCode.Success, "success"));

    }

}
