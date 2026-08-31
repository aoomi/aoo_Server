package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import com.ddm.server.common.utils.CommTime;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubLevelRoomLogZhongZhiBeforeDayFlow;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.Arrays;
import java.util.List;

@Service(source = "clark_log")
public class ClubLevelRoomLogZhongZhiBeforeDayFlowService implements BaseService<ClubLevelRoomLogZhongZhiBeforeDayFlow> {
    private BaseClarkLogDao<ClubLevelRoomLogZhongZhiBeforeDayFlow> clubLevelRoomLogFlowDao = new BaseClarkLogDao<>(ClubLevelRoomLogZhongZhiBeforeDayFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubLevelRoomLogFlowDao;
    }


    /**
     * 查询所有（同步
     *
     * @param criteria   criteria 策略器
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return
     */
    public <E> List<E> getRoomSizeList(Criteria criteria, Class<E> clazz, String selectHead) {
        String sql = "select t.clubId,t.date_time,sum(t.roomId > 0) as roomSize from (SELECT roomId,clubId,date_time FROM `ClubLevelRoomLog` where date_time = ?  GROUP BY roomId,clubId) as t GROUP BY t.clubId";
        return (List<E>) getDefaultDao().listBeanByClass(sql, clazz, Arrays.asList(CommTime.getYesterDayStringYMD(1)).toArray(new Object[1]));
    }

    /**
     * 查询所有（同步
     *
     * @param criteria   criteria 策略器
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @return
     */
    public <E> List<E> getRoomSizeList(Criteria criteria, String dateTime, Class<E> clazz, String selectHead) {
        String sql = "select t.clubId,t.date_time,sum(t.roomId > 0) as roomSize from (SELECT roomId,clubId,date_time FROM `ClubLevelRoomLogZhongZhi" + dateTime + "` where date_time = ?  GROUP BY roomId,clubId) as t GROUP BY t.clubId";
        return (List<E>) getDefaultDao().listBeanByClass(sql, clazz, Arrays.asList(dateTime).toArray(new Object[1]));
    }

}




