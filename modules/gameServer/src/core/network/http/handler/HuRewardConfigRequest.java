package core.network.http.handler;


import business.global.config.HuRewardConfigMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.db.entity.clarkGame.HuRewardConfigBO;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.config.HuRewardConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 配置文件
 *
 * @author Administrator
 */
public class HuRewardConfigRequest {
    private Object Lock = new Object();

    /**
     * 获取胡牌奖励配置列表数据
     */
    @RequestMapping(uri = "/getHuRewardConfigList")
    public void getHuRewardConfigList(HttpRequest request, HttpResponse response) throws Exception {
        // 获取所有活动信息列表
        List<HuRewardConfig> aInfos = HuRewardConfigMgr.getInstance().getHuRewardConfigList();
        response.response(ZleData_Result.make(ErrorCode.Success, aInfos));
    }

    /**
     * 删除胡牌奖励配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/delHuRewardConfig")
    public void delHuRewardConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            // 检查ID
            if (gameType < 0) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "id < 0 : " + gameType));
                return;
            }

            try {
                // 更新活动状态
                if (HuRewardConfigMgr.getInstance().delHuRewardConfig(gameType)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "del error or gameType error"));
                }
            } catch (Exception e) {
                CommLogD.error("delHuRewardConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 更新胡牌奖励配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/updateHuRewardConfig")
    public void updateHuRewardConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            int beginTime = HttpUtils.getInt(dataJson, "beginTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");
            String prize = HttpUtils.getString(dataJson, "prize");
            // 奖励列表 == null
            if (StringUtils.isEmpty(prize)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "prize not null"));
                return;
            }

            // 启动时间大于等于结束时间
            if (beginTime >= endTime) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "beginTime >= endTime"));
                return;
            }


            try {
                // 更新活动状态
                if (HuRewardConfigMgr.getInstance().updateHuRewardConfig(gameType, beginTime, endTime, prize)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "update error or gameType error"));
                }
            } catch (Exception e) {
                CommLogD.error("updateHuRewardConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }


        }
    }


    /**
     * 增加胡牌奖励配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/insertHuRConfig")
    public void insertHuRConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            int beginTime = HttpUtils.getInt(dataJson, "beginTime");
            int endTime = HttpUtils.getInt(dataJson, "endTime");
            String prize = HttpUtils.getString(dataJson, "prize");
            // 奖励列表 == null
            if (StringUtils.isEmpty(prize)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "prize not null"));
                return;
            }

            // 启动时间大于等于结束时间
            if (beginTime >= endTime) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "beginTime >= endTime"));
                return;
            }

            try {
                HuRewardConfigBO huConfigBO = new HuRewardConfigBO();
                huConfigBO.setGameType(gameType);
                huConfigBO.setBeginTime(beginTime);
                huConfigBO.setEndTime(endTime);
                huConfigBO.setPrize(prize);
                huConfigBO.setCreateTime(CommTime.nowSecond());
                // 添加数据 添加胡牌类型奖励配置
                if (HuRewardConfigMgr.getInstance().insertHuRConfig(huConfigBO)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "insert error"));
                }
            } catch (Exception e) {
                CommLogD.error("insertHuRConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }
}
