package business.global.sharediscount;

import BaseCommon.CommLog;
import com.ddm.server.common.redis.RedisMap;
import com.google.gson.Gson;
import core.db.entity.clarkGame.DiscountBO;
import core.ioc.ContainerMgr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author xsj
 * @date 2020/12/18 15:11
 * @description 共享打折（免费）活动管理类
 */
public class ShareDiscountMgr {
    private static final String SHARE_DISCOUNT_KEY = "shareDiscountKey";
    private static ShareDiscountMgr instance = new ShareDiscountMgr();

    // 获取单例
    public static ShareDiscountMgr getInstance() {
        return instance;
    }

    /**
     * 添加打折（免费）活动
     *
     * @param discountBO)
     */
    public void addDiscount(DiscountBO discountBO) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_DISCOUNT_KEY);
        redisMap.putJson(String.valueOf(discountBO.getId()), discountBO);
    }

    /**
     * 删除打折（免费）活动
     *
     * @param id
     */
    public void deleteDiscount(Long id) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_DISCOUNT_KEY);
        redisMap.removeObject(String.valueOf(id));
    }

    /**
     * 获取所有打折（免费）活动
     *
     * @return
     */
    public Map<Long, DiscountBO> getAllDiscount() {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_DISCOUNT_KEY);
        Set<Map.Entry<String, String>> allSet = redisMap.entrySet();
        if (allSet != null) {
            Map<Long, DiscountBO> discountBOMap = new HashMap<>(allSet.size());
            Gson gson = new Gson();
            allSet.forEach(map -> {
                try {
                    DiscountBO discountBO = gson.fromJson(map.getValue(), DiscountBO.class);
                    discountBOMap.put(Long.parseLong(map.getKey()), discountBO);
                } catch (Exception e) {
                    System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                    CommLog.error(e.getMessage(), e);
                }
            });
            return discountBOMap;
        } else {
            return new HashMap<>(1);
        }
    }

    /**
     * 获取一个打折（免费）活动
     *
     * @param id
     */
    public DiscountBO getDiscountBO(Long id) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_DISCOUNT_KEY);
        return redisMap.getObject(String.valueOf(id), DiscountBO.class);
    }

}
