package core.network.http.handler;

import business.global.config.TaskConfigMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.db.entity.clarkGame.TaskConfigBO;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskClassify;
import jsproto.c2s.cclass.task.TaskConfigEnum.TaskTargetType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 配置文件
 *
 * @author Administrator
 */
public class TaskConfigRequest {
    private Object Lock = new Object();

    /**
     * 获取列表数据
     */
    @RequestMapping(uri = "/getTaskList")
    public void getActivityTaskConfigList(HttpRequest request, HttpResponse response) throws Exception {
        try {
            // 获取所有活动信息列表
            List<TaskConfigBO> aInfos = TaskConfigMgr.getInstance().getTaskList();
            response.response(ZleData_Result.make(ErrorCode.Success, aInfos));
        } catch (Exception e) {
            CommLogD.error("getTaskList error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }

    /**
     * 移除一个活动任务
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/delTask")
    public void delActivityTask(HttpRequest request, HttpResponse response) throws Exception {
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
                if (TaskConfigMgr.getInstance().delTask(id)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "del error or taskType error"));
                }
            } catch (Exception e) {
                CommLogD.error("delTask error : {}", e.getMessage());
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
    @RequestMapping(uri = "/setTask")
    public void setTask(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            long id = HttpUtils.getLong(dataJson, "id");
            int taskType = HttpUtils.getInt(dataJson, "taskType");
            String title = HttpUtils.getString(dataJson, "title");
            String content = HttpUtils.getString(dataJson, "content");
            String url = HttpUtils.getString(dataJson, "url");
            int targetValue = HttpUtils.getInt(dataJson, "targetValue");
            int targetType = HttpUtils.getInt(dataJson, "targetType");
            long preTaskId = HttpUtils.getLong(dataJson, "preTaskId");
            String reward = HttpUtils.getString(dataJson, "reward");
            int timeType = HttpUtils.getInt(dataJson, "timeType");
            int startTime = HttpUtils.getInt(dataJson, "startTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");

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
            if (TaskTargetType.None.equals(TaskTargetType.valueOf(targetType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "targetType < 0"));
                return;
            }

            // 前置任务ID
            if (preTaskId < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "preTaskId < 0"));
                return;
            }


            // reward == null
            if (StringUtils.isEmpty(reward)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "reward not null"));
                return;
            }

            try {
                // 更新活动任务
                if (TaskConfigMgr.getInstance().setTask(id, taskType, title, content, url, targetValue, targetType, preTaskId, reward, timeType, startTime, endTime)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "setTask error"));
                }
            } catch (Exception e) {
                CommLogD.error("setTask error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

}
