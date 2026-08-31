package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.entity.clarkGame.PlayerBO;
import lombok.NoArgsConstructor;

@Dao
@NoArgsConstructor
public class PlayerBODao extends BaseClarkGameDao<PlayerBO> {

    public Long getCurMaxId() {
        String sql = String.format("SELECT MAX(id) FROM %s", getTableName());
        return getValue(sql);
    }

    @Override
    public long insertAndGetGeneratedKeys(String sql, Object... args) {
        return super.insertAndGetGeneratedKeys(sql, args);
    }
}
