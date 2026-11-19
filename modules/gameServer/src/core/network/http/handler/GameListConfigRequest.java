package core.network.http.handler;

import business.global.config.GameListConfigMgr;
import business.rocketmq.constant.MqTopic;
import cenum.GameOpenTypeEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqAbsBo;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.annotation.RequestMapping;
import com.ddm.server.http.server.HttpRequest;
import com.ddm.server.http.server.HttpResponse;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.JsonObject;
import core.network.http.proto.ZleData_Result;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class GameListConfigRequest {
    /**
     * 重新加载游戏列表配置
     */
    @RequestMapping(uri = "/reloadGameListConfig")
    public void reloadGameListConfig(HttpRequest request, HttpResponse response) throws Exception {
        try {
            MqProducerMgr.get().send(MqTopic.HTTP_RELOAD_GAME_LIST_CONFIG, new MqAbsBo());
            response.response(ZleData_Result.make(ErrorCode.Success, "success"));
        } catch (Exception e) {
            CommLogD.error("reloadGameListConfig error : {}", e.getMessage());
            response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
        }

    }

    private Object Lock = new Object();

    /**
     * 设置游戏类型配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setGameTypeConfig")
    public void setGameTypeConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            String name = HttpUtils.getString(dataJson, "name");
            String logoico = HttpUtils.getString(dataJson, "logoico");
            String barColors = HttpUtils.getString(dataJson, "barColors");
            String gameName = HttpUtils.getString(dataJson, "gameName");
            int have_xifen = HttpUtils.getInt(dataJson, "have_xifen");
            int tab = HttpUtils.getInt(dataJson, "tab");
            String hutypelist = HttpUtils.getString(dataJson, "hutypelist");
            int sort = HttpUtils.getInt(dataJson, "sort");
            int classType = HttpUtils.getInt(dataJson, "classType");
            String gameServerIP = HttpUtils.getString(dataJson, "gameServerIP");
            int gameServerPort = HttpUtils.getInt(dataJson, "gameServerPort");
            String webSocketUrl = HttpUtils.getString(dataJson, "webSocketUrl");
            String httpUrl = HttpUtils.getString(dataJson, "httpUrl");
            int openType = HttpUtils.getInt(dataJson, "openType");
            List<Long> openContent = HttpUtils.getLongList(dataJson, "openContent");


            // 名称 == null
            if (StringUtils.isEmpty(name)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "key not name"));
                return;
            }

            // 图标 == null
            if (StringUtils.isEmpty(logoico)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "key not logoico"));
                return;
            }

            // 颜色 == null
            if (StringUtils.isEmpty(barColors)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "key not barColors"));
                return;
            }

            // GameType == null
            if (StringUtils.isEmpty(gameName)) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "key not gameName"));
                return;
            }

            if (Objects.isNull(GameOpenTypeEnum.valueOf(openType))) {
                response.response(ZleData_Result.make(ErrorCode.InvalidParam, "key not openType"));
                return;
            }
            try {
                response.response(GameListConfigMgr.getInstance().set(gameType, name, logoico, barColors, gameName, have_xifen, tab, hutypelist, sort, classType, gameServerIP, gameServerPort, webSocketUrl, httpUrl, openType, openContent));
            } catch (Exception e) {
                CommLogD.error("setGameTypeConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 设置游戏类型配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setGameTypeTab")
    public void setGameTypeTab(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            int tab = HttpUtils.getInt(dataJson, "tab");
            try {
                response.response(GameListConfigMgr.getInstance().setTab(gameType, tab));
            } catch (Exception e) {
                CommLogD.error("setGameTypeConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 开放游戏
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setOpenTypeAndContent")
    public void setOpenTypeAndContent(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            int tab = HttpUtils.getInt(dataJson, "tab");
            int openType = HttpUtils.getInt(dataJson, "openType");
            List<Long> openContent = HttpUtils.getLongList(dataJson, "openContent");
            try {
                response.response(GameListConfigMgr.getInstance().openTypeAndContent(gameType, tab, openType, openContent));
            } catch (Exception e) {
                CommLogD.error("setOpenTypeAndContent error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }

    /**
     * 设置游戏类型排序等级
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setGameTypeSort")
    public void setGameTypeSort(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            int sort = HttpUtils.getInt(dataJson, "sort");

            try {
                if (GameListConfigMgr.getInstance().setGameHuTypeSort(gameType, sort)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("setGameHuTypeList error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 设置游戏胡类型列表
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/setGameHuTypeList")
    public void setGameHuTypeList(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            String hutypelist = HttpUtils.getString(dataJson, "hutypelist");
            try {
                if (GameListConfigMgr.getInstance().setGameHuTypeList(gameType, hutypelist)) {
                    response.response(ZleData_Result.make(ErrorCode.Success, "success"));
                } else {
                    response.response(ZleData_Result.make(ErrorCode.NotAllow, "error"));
                }
            } catch (Exception e) {
                CommLogD.error("setGameHuTypeList error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


    /**
     * 删除指定游戏类型配置
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(uri = "/deleteGameTypeConfig")
    public void deleteGameTypeConfig(HttpRequest request, HttpResponse response) throws Exception {
        String jsonPost = StringUtil.GetStringBody(request.getRequestBody());
        JsonObject dataJson = HttpUtils.abstractGMParams(jsonPost, HttpUtils.ZLE_KEY_RECHARGE);
        synchronized (Lock) {
            int gameType = HttpUtils.getInt(dataJson, "gameType");
            try {
                response.response(GameListConfigMgr.getInstance().delete(gameType));
            } catch (Exception e) {
                CommLogD.error("deleteGameTypeConfig error : {}", e.getMessage());
                response.response(ZleData_Result.make(ErrorCode.ErrorSysMsg, "null"));
            }

        }
    }


}
