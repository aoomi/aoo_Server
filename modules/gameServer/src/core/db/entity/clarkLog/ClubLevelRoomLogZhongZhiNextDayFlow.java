package core.db.entity.clarkLog;

import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.CommTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "ClubLevelRoomLogZhongZhi", dbDay = TableName.DbDayEnum.NEXT_DAY)
@Data
@NoArgsConstructor
public class ClubLevelRoomLogZhongZhiNextDayFlow extends ClubLevelRoomLogZhongZhiFlow {

    @Override
    public String getInsertSql() {
        return super.getInsertSql();
    }

    public static String getCreateTableSQL() {
        return getCreateTableSQL(String.valueOf(CommTime.getNextTime6YMD()));
    }

    @Override
    public Object[] addToBatch() {
        return super.addToBatch();
    }


}
