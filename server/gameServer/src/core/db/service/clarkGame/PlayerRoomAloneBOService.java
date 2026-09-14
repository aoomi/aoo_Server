package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRoomAloneBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerRoomAloneBOService implements BaseService<PlayerRoomAloneBO> {
    private BaseClarkGameDao<PlayerRoomAloneBO> gameRoomPlayerBODao = new BaseClarkGameDao<>(PlayerRoomAloneBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return gameRoomPlayerBODao;
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
        String where = "PlayerRoomAlone";
        // 内部查询
        String sql = String.format("select %s from %s where %s group by roomID", selectHead, where, criteria.toSql());
        // 外包查询
        String clubTotalInfo = String.format("select count(t.roomTotalCount) as roomTotalCount,sum(t.roomCardTotalCount) as roomCardTotalCount from (%s) as t", sql);
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return (E) getDefaultDao().getBeanByClass(clubTotalInfo, clazz, objects);
    }

    /**
     * 查询单条数据（实体含有List类型接收Db多个字段就需要转化，设置needConvert=true）
     *
     * @param criteria 查询策略
     * @return 返回查询实体对象
     */
    public Long countTest(Criteria criteria, String selectHead) {
        // 条件
        String table = "PlayerRoomAlone";
        // 内部查询
        String sql = String.format("select %s from %s where %s", selectHead, table, criteria.toSql());
        // 外包查询
        String clubTotalInfo = String.format("select count(1) from (%s) as t", sql);
        Object[] objects = criteria.getParams().toArray(new Object[criteria.getParams().size()]);
        return (Long) getDefaultDao().aggregation(clubTotalInfo, objects);
    }

}
