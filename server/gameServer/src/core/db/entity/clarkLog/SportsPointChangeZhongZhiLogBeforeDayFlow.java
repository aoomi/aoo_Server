package core.db.entity.clarkLog;

import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "SportsPointChangeZhongZhiLog", dbDay = TableName.DbDayEnum.BEFORE_DAY)
@Data
@NoArgsConstructor
public class SportsPointChangeZhongZhiLogBeforeDayFlow extends SportsPointChangeZhongZhiLogFlow {

    @Override
    public String getInsertSql() {
        return super.getInsertSql();
    }

    public static String getCreateTableSQL() {
        return getCreateTableSQL(CommTime.getBeforeTimeYMD());
    }

    @Override
    public Object[] addToBatch() {
        return super.addToBatch();
    }


}
