package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.entity.clarkGame.RefererReceiveListBO;
import core.db.other.Criteria;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Dao
public class RefererReceiveListBODao extends BaseClarkGameDao<RefererReceiveListBO> {

    public Map<String, Object> findRefererReceiveTotalBO(Criteria criteria) {
        String sql = "select IFNULL((count(r.id),0) as `totalNumber`,IFNULL(sum(p.totalRecharge) ,0) as `totalPrice` from `refererReceiveList` r left join `player` p on r.pid = p.id" + " where " + criteria.toSql();
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return getMap(sql, objects);
    }
}


