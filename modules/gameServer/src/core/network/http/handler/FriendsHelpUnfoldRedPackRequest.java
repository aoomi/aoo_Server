package core.network.http.handler;

import business.global.config.FriendsHelpUnfoldRedPackMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerFriendsHelpUnfoldRedPack;
import cenum.FriendsHelpUnfoldRedPackEnum.PondType;
import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.SData_Result;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskClassify;
import org.apache.commons.lang3.StringUtils;


public class FriendsHelpUnfoldRedPackRequest {
    private Object Lock = new Object();

    /**
     * 获取列表数据
     */
    @RequestMapping(uri = "/getFriendsHelpUnfoldRedPackList")
    public void getFriendsHelpUnfoldRedPackList(HttpRequest request, HttpResponse response) throws Exception {
        try {
            // 获取所有活动信息列表
            response.response(ZleData_Result.make(ErrorCode.Success, FriendsHelpUnfoldRedPackMgr.getInstance().getTaskList()));
        } catch (Exception e) {
            CommLogD.error("getFriendsHelpUnfoldRedPackList error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }

    /**
     * 删除指定任务
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/delFriendsHelpUnfoldRedPack")
    public void delFriendsHelpUnfoldRedPack(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            // 检查ID
            if (id < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "id < 0 : " + id));
                return;
            }
            try {
                // 移除一个活动任务
                if (FriendsHelpUnfoldRedPackMgr.getInstance().delTask(id)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "del error or taskType error"));
                }
            } catch (Exception e) {
                CommLogD.error("delFriendsHelpUnfoldRedPack error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 更新任务
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setFriendsHelpUnfoldRedPack")
    public void setFriendsHelpUnfoldRedPack(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            int taskType = HttpUtils.getInt(dataJson, "taskType");
            String title = HttpUtils.getString(dataJson, "title");
            String content = HttpUtils.getString(dataJson, "content");
            int targetValue = HttpUtils.getInt(dataJson, "targetValue");
            int targetType = HttpUtils.getInt(dataJson, "targetType");
            long preTaskId = HttpUtils.getLong(dataJson, "preTaskId");
            int value = HttpUtils.getInt(dataJson, "value");
            int pondType = HttpUtils.getInt(dataJson, "pondType");


            // 任务ID <= 0
            if (TaskClassify.None.equals(TaskClassify.valueOf(taskType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "taskType < 0"));
                return;
            }

            // 标题 == null
            if (StringUtils.isEmpty(title)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "title not null"));
                return;
            }

            // content == null
            if (StringUtils.isEmpty(content)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "content not null"));
                return;
            }

            // 目标值
            if (targetValue <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "targetValue <= 0"));
                return;
            }

            // 目标类型
            if (TargetType.None.equals(TargetType.valueOf(targetType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "targetType < 0"));
                return;
            }

            // 前置任务ID
            if (preTaskId < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "preTaskId < 0"));
                return;
            }

            if (value <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "value <= 0"));
                return;
            }

            if (pondType <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "pondType <= 0"));
                return;
            }
            try {

                // 更新活动任务
                if (FriendsHelpUnfoldRedPackMgr.getInstance().setTask(id, taskType, title, content, targetValue, targetType, preTaskId, value, pondType)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "setTask error"));
                }
            } catch (Exception e) {
                CommLogD.error("setFriendsHelpUnfoldRedPack error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 更新好友帮拆红包时间配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setFriendsHelpUnfoldRedPackConfig")
    public void setFriendsHelpUnfoldRedPackConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int startTime = HttpUtils.getInt(dataJson, "startTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");
            int limitType = HttpUtils.getInt(dataJson, "limitType");
            int limitTime = HttpUtils.getInt(dataJson, "limitTime");

            if (startTime <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "startTime <= 0"));
                return;
            }
            if (endTime <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "endTime <= 0"));
                return;
            }
            if (limitTime <= 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "limitTime <= 0"));
                return;
            }
            try {
                // 更新活动任务
                if (FriendsHelpUnfoldRedPackMgr.getInstance().setFriendsHelpUnfoldRedPackConfig(startTime, endTime, limitType, limitTime)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "setTask error"));
                }
            } catch (Exception e) {
                CommLogD.error("setFriendsHelpUnfoldRedPackConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 获取好友帮拆红包时间配置
     */
    @RequestMapping(uri = "/getFriendsHelpUnfoldRedPackConfig")
    public void getFriendsHelpUnfoldRedPackConfig(HttpRequest request, HttpResponse response) throws Exception {
        try {
            // 获取所有活动信息列表
            response.response(ZleData_Result.make(ErrorCode.Success, FriendsHelpUnfoldRedPackMgr.getInstance().getFriendsHelpUnfoldRedPackConfigInfo()));
        } catch (Exception e) {
            CommLogD.error("getFriendsHelpUnfoldRedPackConfig error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }


    /**
     * 执行取钱
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @RequestMapping(uri = "/exeDrawMoney")
    public void exeDrawMoney(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            int pondType = HttpUtils.getInt(dataJson, "pondType");
            if (PondType.None.equals(PondType.valueOf(pondType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("pondType:{%d}", pondType)));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (null == player) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("null == player:{%d}", pid)));
                return;
            }
            try {
                SData_Result result = player.getFeature(PlayerFriendsHelpUnfoldRedPack.class).exeDrawMoney(pondType);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, (int) result.getCustom() + ""));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("exeDrawMoney error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }


    /**
     * 检查取钱
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @RequestMapping(uri = "/checkDrawMoney")
    public void checkDrawMoney(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long pid = HttpUtils.getLong(dataJson, "pid");
            int pondType = HttpUtils.getInt(dataJson, "pondType");
            if (PondType.None.equals(PondType.valueOf(pondType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("pondType:{%d}", pondType)));
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(pid);
            if (null == player) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, String.format("null == player:{%d}", pid)));
                return;
            }
            try {
                SData_Result result = player.getFeature(PlayerFriendsHelpUnfoldRedPack.class).checkDrawMoney(pondType);
                if (ErrorCode.Success.equals(result.getCode())) {
                    response.response(ZleData_Result.make(ErrorCode.Success, (int) result.getCustom() + ""));
                } else {
                    response.response(ZleData_Result.make(result.getCode(), result.getMsg()));
                }
            } catch (Exception e) {
                CommLogD.error("exeDrawMoney error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }
        }
    }

}
