package business.global.config;

import business.global.sharegm.ShareNodeServerMgr;
import business.global.shareroom.ShareRoom;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.ShareNode;
import com.alibaba.fastjson.JSON;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqAbsBo;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import core.db.entity.clarkGame.GameTypeBO;
import core.db.service.clarkGame.GameTypeBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.ZleData_Result;
import jsproto.c2s.cclass.GameTypeUrl;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class GameListConfigMgr {


    private static class SingletonHolder {
        public static GameListConfigMgr instance = new GameListConfigMgr();
    }

    public static GameListConfigMgr getInstance() {
        return SingletonHolder.instance;
    }

    private Map<Integer, GameTypeBO> confs = new HashMap<>();

    /**
     * 初始化配置
     */
    public void init() {
        List<GameTypeBO> gameTypeBOs = ContainerMgr.get().getComponent(GameTypeBOService.class).findAll(null);
        if (CollectionUtils.isEmpty(gameTypeBOs)) {
            return;
        }
        for (GameTypeBO gBo : gameTypeBOs) {
            this.confs.put(gBo.getGametype(), gBo);
        }
    }

    /**
     * 添加或设置指定的游戏类型
     *
     * @param gameType   游戏类型
     * @param name       名称
     * @param logoico    图标
     * @param barColors  颜色
     * @param gameName   简称:(LYMJ)
     * @param have_xifen 1:有细分游戏,0没有
     * @param tab        0:不显示,1:默认显示,2:禁用
     */
    public String set(Integer gameType, String name, String logoico, String barColors, String gameName, int have_xifen,
                      int tab, String hutypelist, int sort, int classType, String gameServerIP, int gameServerPort, String webSocketUrl, String httpUrl, int openType, List<Long> openContent) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null == bo) {
            bo = new GameTypeBO();
        }
        bo.setGametype(gameType);
        bo.setName(name);
        bo.setLogoico(logoico);
        bo.setBarColors(barColors);
        bo.setGameName(gameName);
        bo.setHave_xifen(have_xifen);
        bo.setTab(tab);
        bo.setHutypelist(hutypelist);
        bo.setSort(sort);
        bo.setClassType(classType);
        bo.setGameServerIP(gameServerIP);
        bo.setGameServerPort(gameServerPort);
        bo.setWebSocketUrl(webSocketUrl);
        bo.setHttpUrl(httpUrl);
        bo.setOpenType(openType);
        bo.setOpenContent(JSON.toJSONString(CollectionUtils.isEmpty(openContent) ? Collections.emptyList() : openContent));
        bo.setOpenContentList(CollectionUtils.isEmpty(openContent) ? Collections.emptyList() : openContent);
        bo.getBaseService().saveOrUpDate(bo);
        this.confs.put(gameType, bo);
        return ZleData_Result.make(ErrorCode.Success, "success");
    }


    /**
     * 添加或设置指定的游戏类型
     *
     * @param gameType 游戏类型
     * @param tab      0:不显示,1:默认显示,2:禁用
     */
    public String setTab(int gameType, int tab) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null == bo) {
            return ZleData_Result.make(ErrorCode.NotAllow, "error gameType");
        }
        bo.saveTab(tab);
        return ZleData_Result.make(ErrorCode.Success, "success");
    }

    public boolean setGameHuTypeList(Integer gameType, String hutypelist) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null != bo) {
            bo.saveHutypelist(hutypelist);
            return true;
        }
        return false;
    }

    public boolean setGameHuTypeSort(Integer gameType, int sort) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null != bo) {
            bo.saveSort(sort);
            return true;
        }
        return false;
    }


    public String openTypeAndContent(int gameType, int tab, int openType, List<Long> openContent) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null != bo) {
            bo.openTypeAndContent(tab, openType, openContent);
            return ZleData_Result.make(ErrorCode.Success, "success");
        }
        return ZleData_Result.make(ErrorCode.NotAllow, "error openTypeAndContent");
    }

    /**
     * 更新全部备用节点端口
     */
    public void updateAllPort() {
        if (Config.isStartChangePort()) {
            CommLogD.info("切换端口");
            this.confs.forEach((k, v) -> {
                if (v.getGameServerPort() == Config.backUpNodePort()) {
                    v.saveGameServerPort(Config.nodePort());
                }
            });
            MqProducerMgr.get().send(MqTopic.HTTP_RELOAD_GAME_LIST_CONFIG, new MqAbsBo());
        }

    }


    /**
     * 删除指定游戏类型
     *
     * @param gameType
     */
    public String delete(Integer gameType) {
        GameTypeBO bo = this.confs.get(gameType);
        if (null != bo) {
            this.confs.remove(gameType);
            bo.getBaseService().delete(bo.getId());
            return ZleData_Result.make(ErrorCode.Success, "success");
        }
        return ZleData_Result.make(ErrorCode.NotAllow, "error gameType");
    }

    /**
     * 获取所有配置
     *
     * @return
     */
    public Map<Integer, GameTypeBO> getAllConfig() {
        return new HashMap<>(this.confs);
    }

    /**
     * 获取所有配置列表
     *
     * @return
     */
    public List<GameTypeUrl> getAllList() {
        List<GameTypeUrl> gameTypeBOList = this.confs
                .values()
                .stream()
                .map(k -> getGameTypeUrl(k))
                .collect(Collectors.toList());
        return gameTypeBOList;
    }

    /**
     * 获取一个游戏配置
     *
     * @return
     */
    public GameTypeUrl getByGameType(Integer gameType) {
        GameTypeBO gameTypeBO = this.confs.get(gameType);
        return getGameTypeUrl(gameTypeBO);
    }

    /**
     * 根据房间获取服务节点
     *
     * @return
     */
    public GameTypeUrl getByRoom(ShareRoom shareRoom) {
        ShareNode shareNode = shareRoom.getCurShareNode();
        if (shareRoom.isNoneRoom()) {
            return getByGameType(shareRoom.getGameId());
        } else {
            GameTypeUrl gameTypeUrl = new GameTypeUrl();
            gameTypeUrl.setStart(true);
            gameTypeUrl.setGametype(shareRoom.getGameId());
            gameTypeUrl.setWebSocketUrl(shareNode.getVipAddress());
            gameTypeUrl.setGameServerIP(shareNode.getIp());
            gameTypeUrl.setGameServerPort(shareNode.getPort());
            return gameTypeUrl;
        }
    }

    /**
     * 获取游戏节点
     *
     * @param shareRoom
     * @return
     */
    public ShareNode getShareNodeByRoom(ShareRoom shareRoom) {
        GameTypeUrl gameTypeUrl = getByRoom(shareRoom);
        ShareNode shareNode = new ShareNode("", gameTypeUrl.getWebSocketUrl(), gameTypeUrl.getGameServerIP(), gameTypeUrl.getGameServerPort());
        return shareNode;
    }

    /**
     * 检查游戏所属节点是否存活
     *
     * @param gameType
     * @return
     */
    public boolean checkIsLiveByGameType(Integer gameType) {
        GameTypeBO gameTypeBO = this.confs.get(gameType);
        return ShareNodeServerMgr.getInstance().checkIsLiveByIpPort(gameTypeBO.getGameServerIP(), gameTypeBO.getGameServerPort());
    }

    /**
     * 检查游戏所属节点是否存活
     *
     * @param shareRoom
     * @return
     */
    public boolean checkIsLiveByRoom(ShareRoom shareRoom) {
        ShareNode shareNode = shareRoom.getCurShareNode();
        if (shareRoom.isNoneRoom()) {
            return checkIsLiveByGameType(shareRoom.getGameId());
        } else {
            return ShareNodeServerMgr.getInstance().checkIsLiveByIpPort(shareNode.getIp(), shareNode.getPort());
        }

    }

    private GameTypeUrl getGameTypeUrl(GameTypeBO bo) {
        if (bo == null) {
            return null;
        }
        GameTypeUrl gameTypeUrl = new GameTypeUrl();
        gameTypeUrl.setGametype(bo.getGametype());
        gameTypeUrl.setHttpUrl(bo.getHttpUrl());
        gameTypeUrl.setWebSocketUrl(bo.getWebSocketUrl());
        gameTypeUrl.setGameServerIP(bo.getGameServerIP());
        gameTypeUrl.setGameServerPort(bo.getGameServerPort());
        gameTypeUrl.setStart(ShareNodeServerMgr.getInstance().checkIsLiveByIpPort(bo.getGameServerIP(), bo.getGameServerPort()));
        return gameTypeUrl;
    }

}
