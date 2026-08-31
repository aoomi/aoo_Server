package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import com.ddm.server.common.utils.CommTime;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerSignBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.Objects;

@Service(source = "clark_game")
public class PlayerSignBOService implements BaseService<PlayerSignBO> {
    private BaseClarkGameDao<PlayerSignBO> playerSignBODao = new BaseClarkGameDao<>(PlayerSignBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerSignBODao;
    }


    /**
     * 保存或者更新（同步）
     *
     * @return 返回操作结果
     */
    public PlayerSignBO saveIgnoreOrUpDate(long pid, int maxDay) {
        PlayerSignBO signBO = findOne(Restrictions.eq("pid", pid), null);
        if (Objects.isNull(signBO)) {
            signBO = new PlayerSignBO();
            signBO.setCreateTime(CommTime.nowSecond());
            signBO.setPid(pid);
            signBO.setRewardState(1);
            signBO.setSignCount(1);
            signBO.setSignTime(CommTime.nowSecond());
            return saveIgnore(signBO) > 0L ? signBO : null;
        } else {
            int missDays = CommTime.daysBetween(signBO.getSignTime(), CommTime.nowSecond());
            if (missDays <= 0) {
                // 同一天
                return null;
            }
            if (signBO.getSignCount() >= maxDay) {
                // 大于等于最大天数,重置
                signBO.setSignCount(1);
            } else {
                //否则 签到次数 +1
                signBO.setSignCount(signBO.getSignCount() + 1);
            }
            // 设置第二次签到数
            signBO.setSignTime(CommTime.nowSecond());
            signBO.setRewardState(1);
            return update(signBO) > 0 ? signBO : null;
        }


    }
}




