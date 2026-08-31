package core.db.entity.clarkLog;

import com.ddm.server.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Administrator
 */
@TableName(value = "ClubLevelRoomLog", dbDay = TableName.DbDayEnum.NOT_DAY)
@Data
@NoArgsConstructor
public class ClubLevelRoomLogNotDayFlow extends ClubLevelRoomLogFlow {

    @Override
    public String getInsertSql() {
        return super.getInsertSql();
    }

    public static String getCreateTableSQL() {
        return getCreateTableSQL("");
    }

    @Override
    public Object[] addToBatch() {
        return super.addToBatch();
    }


}
