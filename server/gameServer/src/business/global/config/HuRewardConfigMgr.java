package business.global.config;

import BaseThread.BaseMutexObject;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.db.entity.clarkGame.HuRewardConfigBO;
import core.db.entity.clarkGame.HuRewardRecordBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.service.clarkGame.HuRewardConfigBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.config.HuRewardConfig;
import jsproto.c2s.cclass.config.HuRewardGroupInfo;
import jsproto.c2s.cclass.config.HuRewardPrizeInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 胡类型奖励配置
 *
 * @author Huaxing
 */
@Data
public class HuRewardConfigMgr {
    private static HuRewardConfigMgr instance = new HuRewardConfigMgr();
    // 游戏类型的胡牌奖励配置字典。
    private Map<Integer, HuRewardConfig> huRConfigMap = new ConcurrentHashMap<>();
    private HuRewardConfigBOService huRewardConfigBOService;

    public static HuRewardConfigMgr getInstance() {
        return instance;
    }

    private final BaseMutexObject _lock = new BaseMutexObject();

    public void lock() {
        _lock.lock();
    }

    public void unlock() {
        _lock.unlock();
    }

    /**
     * 初始胡牌奖励配置
     */
    public void init() {
        this.setHuRewardConfigBOService(ContainerMgr.get().getComponent(HuRewardConfigBOService.class));
        this.getHuRewardConfigBOService().findAll(null).stream().filter(k -> null != k).forEach(k -> {
            this.getHuRConfigMap().put(k.getGameType(), this.newHuRewardConfig(k));
        });
    }

    /**
     * new 新的胡牌奖励配置
     *
     * @param hConfigBO
     * @return
     */
    public HuRewardConfig newHuRewardConfig(HuRewardConfigBO hConfigBO) {
        HuRewardConfig hConfig = new HuRewardConfig();
        hConfig.setBeginTime(hConfigBO.getBeginTime());
        hConfig.setCreateTime(hConfigBO.getCreateTime());
        hConfig.setEndTime(hConfigBO.getEndTime());
        hConfig.setGameType(hConfigBO.getGameType());
        hConfig.setId(hConfigBO.getId());
        // 获取解析后的配置
        hConfig.setPrize(huRewardPrizeInfoList(hConfigBO.getPrize()));
        return hConfig;
    }


    /**
     * 解析胡牌奖励列表
     *
     * @param huRewardPrizeInfos
     * @return
     */
    public List<HuRewardGroupInfo> huRewardPrizeInfoList(String huRewardPrizeInfos) {
        List<HuRewardGroupInfo> hPrizeInfos = new ArrayList<>();
        // 检查字符串是否为空
        if (StringUtils.isEmpty(huRewardPrizeInfos)) {
            return hPrizeInfos;
        }
        try {
            // 解析奖励信息
            hPrizeInfos = new Gson().fromJson(huRewardPrizeInfos, new TypeToken<List<HuRewardGroupInfo>>() {
            }.getType());
        } catch (Exception e) {
            CommLogD.error("Exception not huRewardPrizeInfos : {}", huRewardPrizeInfos);
            return hPrizeInfos;
        }
        return hPrizeInfos;
    }


    /**
     * 解析胡牌奖励列表
     *
     * @param huRewardPrizeInfos
     * @return
     */
    public HuRewardPrizeInfo huRewardPrizeInfo(String huRewardPrizeInfos) {
        HuRewardPrizeInfo hPrizeInfo = null;
        // 检查字符串是否为空
        if (StringUtils.isEmpty(huRewardPrizeInfos)) {
            return hPrizeInfo;
        }
        try {
            // 解析奖励信息
            hPrizeInfo = new Gson().fromJson(huRewardPrizeInfos, new TypeToken<HuRewardPrizeInfo>() {
            }.getType());
        } catch (Exception e) {
            CommLogD.error("Exception not hPrizeInfo : {}", huRewardPrizeInfos);
            return hPrizeInfo;
        }
        return hPrizeInfo;
    }


    /**
     * 添加数据
     * 添加胡牌类型奖励配置
     */
    public boolean insertHuRConfig(HuRewardConfigBO huRConfigBO) {
        boolean isInsert = false;
        HuRewardConfig hConfig = null;
        try {
            lock();
            // 获取胡牌奖励配置
            hConfig = this.huRConfigMap.get(huRConfigBO.getGameType());
            if (null == hConfig) {
                // new 新的胡牌奖励配置
                hConfig = this.newHuRewardConfig(huRConfigBO);
                if (null != hConfig.getPrize() && hConfig.getPrize().size() > 0) {
                    // 添加胡牌类型奖励配置
                    this.huRConfigMap.put(huRConfigBO.getGameType(), hConfig);
                    huRConfigBO.getBaseService().save(huRConfigBO, new AsyncInfo(huRConfigBO.getId()));
                    isInsert = true;
                } else {
                    isInsert = false;
                }
            } else {
                isInsert = false;
            }


        } finally {
            unlock();
        }
        return isInsert;
    }

    /**
     * 单数据
     */
    public HuRewardConfig getHuRewardConfig(int gameType) {
        // 获取指定类型数据
        HuRewardConfig hConfig = this.huRConfigMap.get(gameType);
        if (null == hConfig) {
            return null;
        }
        return hConfig;

    }

    /**
     * 获取列表数据
     *
     * @return
     */
    public List<HuRewardConfig> getHuRewardConfigList() {
        List<HuRewardConfig> hConfigs = new ArrayList<>();
        hConfigs.addAll(this.huRConfigMap.values());
        return hConfigs;
    }

    /**
     * 删除数据
     *
     * @param gameType 游戏类型
     * @return
     */
    public boolean delHuRewardConfig(int gameType) {
        HuRewardConfig hConfig = this.huRConfigMap.remove(gameType);
        if (null == hConfig) {
            return false;
        }
        this.getHuRewardConfigBOService().delete(Restrictions.eq("gameType", gameType));
        return true;

    }

    /**
     * 检查胡牌奖励
     *
     * @param roomId   房间ID
     * @param setId    局数
     * @param pid      玩家ID
     * @param gameType 游戏类型
     * @param huType   胡类型
     * @return
     */
    public boolean checkHuBeginTime(long roomId, int setId, long pid, int gameType, int huType, long familyID) {
        // 获取胡牌奖励配置
        HuRewardConfig hConfig = this.huRConfigMap.get(gameType);
        if (null == hConfig) {
            return false;
        }
        // 检查当前时间是否在指定的时间区间内
        if (!CommTime.checkTimeIntervale(hConfig.getBeginTime(), hConfig.getEndTime())) {
            return false;
        }

        List<HuRewardGroupInfo> prizeGroupInfo = hConfig.getPrize();
        // 检查胡牌分组配置列表
        if (null == prizeGroupInfo || prizeGroupInfo.size() <= 0) {
            return false;
        }
        // 遍历胡牌奖励分组信息
        for (HuRewardGroupInfo groupInfo : prizeGroupInfo) {
            if (groupInfo.getGroupType() == 1) {
                // 活动人群-所有人
                return this.checkPrizeInfo(groupInfo, roomId, setId, pid, gameType, huType, familyID);
            } else if (groupInfo.getGroupType() == 2) {
                // 活动人群-列表中指定的代理旗下的所有成员
                if (groupInfo.getGroupList().contains(familyID)) {
                    return this.checkPrizeInfo(groupInfo, roomId, setId, pid, gameType, huType, familyID);
                }
            } else if (groupInfo.getGroupType() == 3) {
                // 活动人群-除了列表中指定的代理旗下的所有成员
                if (!groupInfo.getGroupList().contains(familyID)) {
                    return this.checkPrizeInfo(groupInfo, roomId, setId, pid, gameType, huType, familyID);
                }
            }
        }
        return false;
    }

    /**
     * 检查胡牌奖励信息,并记录奖励
     *
     * @param groupInfo
     * @param roomId
     * @param setId
     * @param pid
     * @param gameType
     * @param huType
     * @param familyID
     * @return
     */
    private boolean checkPrizeInfo(HuRewardGroupInfo groupInfo, long roomId, int setId, long pid, int gameType, int huType, long familyID) {
        List<HuRewardPrizeInfo> prizeList = groupInfo.getPrizeInfo();
        // 检查胡牌奖励类型配置列表
        if (null == prizeList || prizeList.size() <= 0) {
            return false;
        }
        for (HuRewardPrizeInfo prizeInfo : prizeList) {
            if (prizeInfo.getHuType() == huType) {
                // 添加胡牌奖励记录
                this.insertHuRewardRecordBO(roomId, setId, pid, gameType, prizeInfo, huType);
                return true;
            }
        }
        return false;
    }


    /**
     * 添加胡牌奖励记录
     *
     * @param roomId   房间ID
     * @param setId    局数ID
     * @param pid      玩家ID
     * @param gameType 游戏类型
     * @param prize    奖励
     * @param huType   胡牌类型
     */
    public void insertHuRewardRecordBO(long roomId, int setId, long pid, int gameType, HuRewardPrizeInfo prizeInfo, int huType) {
        HuRewardRecordBO hRecordBO = new HuRewardRecordBO();
        hRecordBO.setPid(pid);
        hRecordBO.setRoomId(roomId);
        hRecordBO.setSetId(setId);
        hRecordBO.setGameType(gameType);
        String prizeInfoStr = new Gson().toJson(prizeInfo);
        hRecordBO.setPrize(prizeInfoStr);
        hRecordBO.setHuType(huType);
        hRecordBO.setCreateTime(CommTime.nowSecond());
        hRecordBO.getBaseService().save(hRecordBO);
    }

    /**
     * 更新 胡牌奖励配置
     *
     * @param gameType
     * @param beginTime
     * @param endTime
     * @param prize
     * @return
     */
    public boolean updateHuRewardConfig(int gameType, int beginTime, int endTime, String prize) {
        boolean isUpdate = false;
        HuRewardConfigBO hBo = this.getHuRewardConfigBOService().findOne(Restrictions.eq("gameType", gameType), null);
        if (null == hBo) {
            return isUpdate;
        }
        try {
            lock();
            hBo.setBeginTime(beginTime);
            hBo.setEndTime(endTime);
            hBo.setPrize(prize);
            HuRewardConfig hConfig = null;
            // new 新的胡牌奖励配置
            hConfig = this.newHuRewardConfig(hBo);
            if (hConfig.getPrize().size() > 0) {
                // 添加胡牌类型奖励配置
                this.huRConfigMap.put(gameType, hConfig);
                hBo.getBaseService().update(hBo);
                isUpdate = true;
            } else {
                isUpdate = false;
            }
        } finally {
            unlock();
        }
        return isUpdate;
    }


}

