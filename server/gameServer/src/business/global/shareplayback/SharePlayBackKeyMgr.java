package business.global.shareplayback;

import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import core.ioc.ContainerMgr;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xsj
 * @date 2020/9/25 16:59
 * @description 回访码共享管理类
 */
public class SharePlayBackKeyMgr {
    //房间存储KEy
    private static final String SHARE_PLAY_BACK_KEY = "sharePlayerBackKey";

    private static SharePlayBackKeyMgr instance = new SharePlayBackKeyMgr();

    // 获取单例
    public static SharePlayBackKeyMgr getInstance() {
        return instance;
    }

    /**
     * 初始化key
     */
    private void init(String playBackKey) {
        String code = getDatePlayBackKey(playBackKey);
        if (StringUtils.isEmpty(code)) {
            setDatePlayBackKey(playBackKey, CommMath.randomInt(100000, 500000));
        }
    }

    /**
     * 设置今天最新的回放码
     *
     * @param key
     */
    public void setDatePlayBackKey(String playBackKey, int key) {
        RedisSource redisSource = ContainerMgr.get().getRedis();
        redisSource.put(playBackKey, key + "");
    }

    /**
     * 获取今天最新的回放码
     *
     * @return
     */
    public String getDatePlayBackKey(String playBackKey) {
        RedisSource redisSource = ContainerMgr.get().getRedis();
        String value = redisSource.get(playBackKey);
        return value;
    }

    /**
     * 获取最新的回放码调用
     *
     * @return
     */
    public Long getNewKey() {
        String playBackKey = RedisBydrKeyEnum.DAILY_PLAY_BACK_KEY.getKey(CommTime.getNowTimeStringYMD());
        Long code = getNewKeyOne(playBackKey);
        if (code == null) {
            code = getInitNewKey(playBackKey);
        }
        return code;

    }

    /**
     * 获取最新一个回放码
     *
     * @return
     */
    private Long getNewKeyOne(String playBackKey) {
        if (getNewKeyOneExist(playBackKey)) {
            return ContainerMgr.get().getRedis().incrLong(playBackKey);
        } else {
            return null;
        }
    }

    /**
     * 判定key是否存在
     *
     * @return
     */
    private Boolean getNewKeyOneExist(String playBackKey) {
        return ContainerMgr.get().getRedis().exists(playBackKey);
    }

    /**
     * 没有获取到回放码初始化一个key
     *
     * @return
     */
    private synchronized Long getInitNewKey(String playBackKey) {
        Long code = getNewKeyOne(playBackKey);
        if (code == null) {
            this.init(playBackKey);
            code = getNewKeyOne(playBackKey);
        }
        return code;
    }

    /**
     * 清除Key,每天凌晨执行
     */
    public void remove() {
//        ContainerMgr.get().getRedis().remove(SHARE_PLAY_BACK_KEY);
    }

}
