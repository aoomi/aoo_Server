package core.db.service.dbZle;

import com.ddm.server.annotation.Service;
import core.db.dao.dbZle.BaseDbZleDao;
import core.db.entity.dbZle.DbPopupBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service(source = "db_zle")
public class DbPopupBOService implements BaseService<DbPopupBO> {
    private BaseDbZleDao<DbPopupBO> dbPopupBODao = new BaseDbZleDao<>(DbPopupBO.class);

    /**
     * 弹窗查询
     *
     * @param isTodayFirst        今日首日登录
     * @param needOnlyPushOneTime 是否是推送
     * @param family              工会
     * @return
     */
    public Optional<List<DbPopupBO>> findAllByTime(boolean isTodayFirst, boolean needOnlyPushOneTime, long family) {
        Criteria criteria = Restrictions.and(Restrictions.caseWhen(new HashMap<String, String>() {{
                    put("pupopType = 2", "true");
                    put("pupopType = 3", !isTodayFirst + "");
                }}, needOnlyPushOneTime + ""), Restrictions.caseWhen(new HashMap<String, String>() {{
                    put("crowdtype = 2", String.format("FIND_IN_SET('%s',crowdlist)", family));
                    put("crowdtype = 3", String.format("!FIND_IN_SET('%s',crowdlist)", family));
                }}, "true"),
                Restrictions.le("star", System.currentTimeMillis() / 1000),
                Restrictions.ge("end", System.currentTimeMillis() / 1000));
        criteria.asc("sort");
        return Optional.ofNullable(dbPopupBODao.findAll(criteria, null, null));
    }

    @Override
    public CustomerDao<DbPopupBO> getDefaultDao() {
        return dbPopupBODao;
    }
}

