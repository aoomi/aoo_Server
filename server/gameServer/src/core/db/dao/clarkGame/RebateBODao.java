package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.entity.clarkGame.RebateBO;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Dao
public class RebateBODao extends BaseClarkGameDao<RebateBO> {
    public int RebateSumFlag(long accountID) {
        String sql = "select sum(rebate.app_price) from rebate where accountID = " + accountID + " and ((flag = 0 and rebateType = 1) or rebateType = 3 or rebateType = 4)";
        return getValue(sql);
    }
}


