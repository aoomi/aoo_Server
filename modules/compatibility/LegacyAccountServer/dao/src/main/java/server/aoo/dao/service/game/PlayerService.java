package server.aoo.dao.service.game;

import cenum.CommonFieldEnum;
import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.redis.jedis.RedisMap;
import com.ddm.server.redis.jedis.RedisSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.aoo.dao.config.util.Restrictions;
import server.aoo.dao.entity.game.DbPlayer;
import server.aoo.dao.mapper.game.DbPlayerDao;
import server.aoo.dao.page.PageUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
public class PlayerService extends BaseGameService {

    @Autowired
    private DbPlayerDao dbPlayerDao;

    public boolean existAccountId(long accountId) {
        DbPlayer playerBO = this.dbPlayerDao.existAccountId(accountId);
        // 找不到玩家数据并且记录玩家账号服id对应的pid
        if (Objects.nonNull(playerBO) && playerBO.getId() > 0L && playerBO.getAccountId() == accountId && RedisSource.getMap(RedisBydrKeyEnum.AID_2_PID_MAP.getKey()).putIf(String.valueOf(accountId), String.valueOf(playerBO.getId()))) {
            RedisMap redisMap = RedisSource.getMap(RedisBydrKeyEnum.PLAYER_MAP.getKey(playerBO.getId()));
            if (redisMap.isEmpty()) {
                return redisMap.beanToMap(playerBO);
            }
            return true;
        }
        return false;
    }

    /**
     * 将数据库数据放到redis中
     * @param pid
     * @return
     */
    public boolean resetDbPlayerToRedisMap(long pid) {
        if(RedisSource.getMap(RedisBydrKeyEnum.PLAYER_MAP.getKey(pid)).isNotEmpty()) {
            // redis 中有数据
            return true;
        } else {
            // 查询玩家
            DbPlayer dbPlayer = this.dbPlayerDao.findOne(pid);
            if (Objects.nonNull(dbPlayer) && dbPlayer.getId() > 0L && RedisSource.getMap(RedisBydrKeyEnum.PLAYER_MAP.getKey(pid)).beanToMap(dbPlayer)) {
                // 记录玩家数据到redis中
                return RedisSource.getMap(RedisBydrKeyEnum.AID_2_PID_MAP.getKey()).putIf(String.valueOf(dbPlayer.getAccountId()), String.valueOf(dbPlayer.getId()));
            }
            // 玩家不存在
            return false;
        }
    }


    /**
     * 检查是否存在
     *
     * @param name
     * @return
     */
    public boolean existName(String name) {
        DbPlayer dbPlayer = this.dbPlayerDao.findOne(Restrictions.eq("name", name), DbPlayer.class, "id");
        return Objects.nonNull(dbPlayer) && dbPlayer.getId() > 0L;
    }


    /**
     * 检查是否存在
     *
     * @param phone
     * @return
     */
    public boolean existPhone(String phone) {
        DbPlayer dbPlayer = this.dbPlayerDao.findOne(Restrictions.eq("phone", phone), DbPlayer.class, "id");
        return Objects.nonNull(dbPlayer) && dbPlayer.getId() > 0L;
    }



    /**
     * 保存Gm等级
     *
     * @param pid
     * @param level
     */
    public void saveGmLevel(long pid, int level) {
        dbPlayerDao.updateGmLevelById(level, pid);
    }


    /**
     * 玩家是否设置“隐藏成就”
     *
     * @param pid
     * @param showProfile
     */
    public void saveShowProfile(long pid, int showProfile) {
        dbPlayerDao.updateShowProfileById(showProfile, pid);
    }

    /**
     * 保存名称
     *
     * @param pid
     * @param name
     */
    public boolean saveName(long pid, String name, int nowSecond) {
        return dbPlayerDao.updateNameById(name, nowSecond, pid) > 0;
    }

    /**
     * 保存邮箱
     *
     * @param pid
     * @param name
     */
    public boolean saveEmailAdree(long pid, String name) {
        return dbPlayerDao.updateEmailAdreeById(name, pid) > 0;
    }

    /**
     * 保存邮箱
     *
     * @param pid
     */
    public String searchEmailAdree(long pid) {
        return dbPlayerDao.searchEmailAdress(pid);
    }

    /**
     * 保存头像
     *
     * @param pid
     * @param headImageUrl
     */
    public boolean saveHeadImageUrl(long pid, String headImageUrl) {
        return dbPlayerDao.updateHeadImageUrlById(headImageUrl, pid) > 0;
    }


    /**
     * 保存手机号
     *
     * @param pid       玩家Pid
     * @param phone 手机号
     */
    public void savePhone(long pid, String phone) {
        dbPlayerDao.updatePhoneById(phone, pid);
    }


    /**
     * 保存禁止登陆设置
     *
     * @param pid       玩家Pid
     * @param bannedInt 禁止登陆时间
     */
    public void saveBannedLogin(long pid, int bannedInt) {
        dbPlayerDao.updateBannedLoginById(bannedInt, pid);
    }

    /**
     * 保存刷新时间
     *
     * @param pid         玩家Pid
     * @param refreshTime 刷新时间
     */
    public void saveRefreshTime(long pid, int refreshTime) {
        dbPlayerDao.updateRefreshTimeById(refreshTime, pid);
    }


    /**
     * 保存等级和最近游戏时间
     *
     * @param pid          玩家Pid
     * @param vipExp       等级
     * @param lastGameTime 最近游戏时间
     */
    public void saveVipExpAndLastGameTime(long pid, int vipExp, int lastGameTime) {
        dbPlayerDao.updateVipExpAndLastGameTimeById(vipExp, lastGameTime, pid);
    }

    /**
     * 保存等级和最近游戏时间
     *
     * @param pid          玩家Pid
     * @param vipLevel       等级
     */
    public void saveVipLevel(long pid, int vipLevel) {
        dbPlayerDao.updateVipLevelById(vipLevel, pid);
    }

    /**
     * 保存等级和活跃时间
     *
     * @param pid                 玩家Pid
     * @param vipExp              等级
     * @param activeRecordingTime 活跃时间
     */
    public void saveVipExpActiveRecordingTime(long pid, int vipExp, int activeRecordingTime) {
        dbPlayerDao.updateVipExpAndActiveRecordingTimeById(vipExp, activeRecordingTime, pid);
    }

    /**
     * 保存最近游戏时间
     *
     * @param pid                 玩家Pid
     * @param activeRecordingTime 活跃时间
     */
    public void saveActiveRecordingTime(long pid, int activeRecordingTime) {
        dbPlayerDao.updateActiveRecordingTimeById(activeRecordingTime, pid);
    }

    /**
     * 保存最近游戏时间
     *
     * @param pid          玩家Pid
     * @param lastGameTime 最近游戏时间
     */
    public void saveLastGameTime(long pid, int lastGameTime) {
        dbPlayerDao.updateLastGameTimeById(lastGameTime, pid);
    }

    /**
     * 保存最近登录时间
     *
     * @param pid       玩家Pid
     * @param lastLogin 最近登录时间
     */
    public void saveLastLogin(long pid, int lastLogin) {
        dbPlayerDao.updateLastLoginById(lastLogin, pid);
    }

    /**
     * 保存筹码
     *
     * @param pid     玩家pid
     * @param maxChip 筹码
     */
    public void saveMaxChip(long pid, double maxChip) {
        dbPlayerDao.updateMaxChipById(maxChip, pid);
    }

    /**
     * 保存总局数
     *
     * @param pid   玩家Pid
     * @param value 数
     */
    public void saveTotalSetCount(long pid, int value) {
        dbPlayerDao.updateTotalSetCountById(value, pid);
    }


    /**
     * 保存胜利数
     *
     * @param pid   玩家Pid
     * @param value 数
     */
    public void saveWinSetCount(long pid, int value) {
        dbPlayerDao.updateWinSetCountById(value, pid);
    }

    /**
     * 保存充值金额
     *
     * @param pid
     * @param value
     */
    public void saveTotalRecharge(long pid, int value) {
        dbPlayerDao.updateTotalRechargeById(value, pid);
    }


    /**
     * 保存推广员的奖金
     *
     * @param pid
     * @param value
     */
    public void saveReferralBonus(long pid, double value) {
        Map<String,Object > map = new HashMap<>(1);
        map.put("referral_bonus", value);
        dbPlayerDao.update(map,pid);
    }

    /**
     * 推广员的奖励次数
     *
     * @param pid
     * @param value
     */
    public void saveReferralNumber(long pid, int value) {
        Map<String,Object > map = new HashMap<>(1);
        map.put("referral_number", value);
        dbPlayerDao.update(map,pid);
    }

    /**
     * 保存最近登录时间
     *
     * @param pid        玩家Pid
     * @param lastLogout 最近登出时间
     */
    public void saveLastLogout(long pid, int lastLogout) {
        dbPlayerDao.updateLastLogoutById(lastLogout, pid);
    }


    /**
     * 保存绑定奖励
     *
     * @param pid        玩家Pid
     * @param binding 绑定奖励
     */
    public void saveBinding(long pid, int binding) {
        dbPlayerDao.updateBindingById(binding, pid);
    }

    /**
     * 保存绑定推广员
     *
     * @param pid        玩家Pid
     * @param realReferer 绑定奖励
     */
    public void saveRealReferer(long pid, long realReferer) {
        dbPlayerDao.updateRealRefererById(realReferer, pid);
    }


    public List<DbPlayer> list() {
        throw new UnsupportedOperationException("unbounded player listing is disabled; use cursor pages");
    }

    public List<DbPlayer> pageCreated(long createTime,long id,int limit){validatePlayerPage(createTime,id,limit);return dbPlayerDao.pageCreated(createTime,id,limit);}
    public List<DbPlayer> pageLastLogin(long lastLogin,long id,int limit){validatePlayerPage(lastLogin,id,limit);return dbPlayerDao.pageLastLogin(lastLogin,id,limit);}
    private static void validatePlayerPage(long time,long id,int limit){if(time<0||id<0||limit<1||limit>500)throw new IllegalArgumentException("invalid player cursor page");}



    /**
     * 玩家战绩统计
     * @return
     */
    public long getReferralBillboardReferralCount (long realReferer) {
        return this.dbPlayerDao.count(Restrictions.eq("real_referer", realReferer));
    }

}
