package core.db.service.clarkLog;


import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.RoomPromotionPointLogFlow;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


@Service(source = "clark_log")
public class RoomPromotionPointLogFlowService implements BaseService<RoomPromotionPointLogFlow> {
    private BaseClarkLogDao<RoomPromotionPointLogFlow> roomPromotionPointLogFlowBaseClarkLogDao = new BaseClarkLogDao<>(RoomPromotionPointLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return roomPromotionPointLogFlowBaseClarkLogDao;
    }

    public RoomPromotionPointLogFlow findOne(long id) {
        return roomPromotionPointLogFlowBaseClarkLogDao.findOne(Restrictions.and(Restrictions.eq("id", id)), null, null);
    }

    /**
     * 查询所有（同步
     * * @param criteria   criteria 策略器
     *
     * @param clazz      欲执行查询类
     * @param selectHead 查询头，自己拼接，没有就null
     * @param dateTime   查询表的日期
     * @return
     */
    public <E> List<E> getDetailBeforDay(Criteria criteria, Class<E> clazz, String selectHead, String dateTime) {
        StringBuilder sql = new StringBuilder();
        Object[] objects = null;
        sql.append("select " + (!StringUtils.isEmpty(selectHead) ? selectHead : "*") + " from " + getTableName(dateTime));
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return getDefaultDao().listBeanByClass(sql.toString(), clazz, objects);
    }

    /**
     * 根据传入的日期 获得要去哪张表查询数据
     *
     * @param dateTime
     * @return
     */
    private String getTableName(String dateTime) {
        return String.format("`%s`", "RoomPromotionPointLog" + dateTime);
    }

}




