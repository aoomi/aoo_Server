package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.GameRoomBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.List;
import java.util.Map;

@Service(source = "clark_game")
public class GameRoomBOService implements BaseService<GameRoomBO> {
    private BaseClarkGameDao<GameRoomBO> gameRoomBODao = new BaseClarkGameDao<>(GameRoomBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return gameRoomBODao;
    }

    /**
     * 查询出所有的集合（实体含有List类型接收Db多个字段就需要转化，设置needConvert=true）
     *
     * @param criteria   查询策略
     * @param selectHead 查询头
     * @return 返回欲查询实体对象数组
     */
    public List<GameRoomBO> findLeftJoinAll(Criteria criteria, String selectHead) {
        StringBuilder sql = new StringBuilder();
        List<Map<String, Object>> result;
        Object[] objects = null;
        sql.append(String.format("select %s from %s", selectHead, "gameRoom as r left join PlayerRoomAlone as p on r.id = p.roomID"));
        if (criteria != null) {
            sql.append(" where " + criteria.toSql());
            objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        }
        return getDefaultDao().listBean(sql.toString(), objects);
    }

    /**
     * 查询单条数据（实体含有List类型接收Db多个字段就需要转化，设置needConvert=true）
     *
     * @param <E>
     * @param criteria 查询策略
     * @param clazz    欲执行查询类
     * @return 返回查询实体对象
     */
    public <E> E findLeftJoinOneE(Criteria criteria, Class<E> clazz, String selectHead) {
        // 条件
        String where = "gameRoom as r left join PlayerRoomAlone as p on r.id = p.roomID";
        // 内部查询
        String sql = String.format("select %s from %s where %s group by r.id", selectHead, where, criteria.toSql());
        // 外包查询
        String clubTotalInfo = String.format("select count(t.roomTotalCount) as roomTotalCount,sum(t.roomCardTotalCount) as roomCardTotalCount from (%s) as t", sql);
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return (E) getDefaultDao().getBeanByClass(clubTotalInfo, clazz, objects);
    }


    /**
     * 查询单条数据（实体含有List类型接收Db多个字段就需要转化，设置needConvert=true）
     *
     * @param <E>
     * @param criteria 查询策略
     * @param clazz    欲执行查询类
     * @return 返回查询实体对象
     */
    public <E> E findOneETest(Criteria criteria, Class<E> clazz, String selectHead) {
        // 条件
        String where = "gameRoom";
        // 内部查询
        String sql = String.format("select %s from %s where %s group by id", selectHead, where, criteria.toSql());
        // 外包查询
        String clubTotalInfo = String.format("select count(t.roomTotalCount) as roomTotalCount,sum(t.roomCardTotalCount) as roomCardTotalCount from (%s) as t", sql);
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return (E) getDefaultDao().getBeanByClass(clubTotalInfo, clazz, objects);
    }
}
