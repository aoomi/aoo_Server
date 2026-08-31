package core.db.entity.clarkLog;

import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "ClubLevelRoomLogZhongZhi", dbDay = TableName.DbDayEnum.BEFORE_6DAY)
@Data
@NoArgsConstructor
public class ClubLevelRoomLogZhongZhiBeforeDayFlow extends ClubLevelRoomLogZhongZhiFlow {

    @Override
    public String getInsertSql() {
        return super.getInsertSql();
    }

    public static String getCreateTableSQL() {
        return getCreateTableSQL(String.valueOf(CommTime.getBeforeTime6YMD()));
    }

    @Override
    public Object[] addToBatch() {
        return super.addToBatch();
    }


}
