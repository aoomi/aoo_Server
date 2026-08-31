package core.db.entity.dbZle;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "db_clubroomcard")
@Data
@NoArgsConstructor
public class ClubRoomCard extends BaseEntity<ClubRoomCard> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "roomcard", comment = "")
    private int roomcard; //房卡数量
    @DataBaseField(type = "bigint(20)", fieldname = "type", comment = "")
    private int type; //1:拨卡;2:撤回
    @DataBaseField(type = "bigint(20)", fieldname = "clubID", comment = "")
    private long clubID; //俱乐部ID
    @DataBaseField(type = "bigint(20)", fieldname = "fromagentsID", comment = "")
    private long fromagentsID; //俱乐部拥有者ID
    @DataBaseField(type = "bigint(20)", fieldname = "fromlevel", comment = "")
    private int fromlevel; //俱乐部拥有者等级
    @DataBaseField(type = "bigint(20)", fieldname = "from_pre_num", comment = "")
    private int from_pre_num; //代理拨卡或回收前的前值
    @DataBaseField(type = "bigint(20)", fieldname = "from_now_num", comment = "")
    private int from_now_num; //代理现在的房卡数量
    @DataBaseField(type = "bigint(20)", fieldname = "club_pre_num", comment = "")
    private int club_pre_num; //俱乐部拨卡或回收前的前值
    @DataBaseField(type = "bigint(20)", fieldname = "club_now_num", comment = "")
    private int club_now_num; //俱乐部现有房卡数量
    @DataBaseField(type = "bigint(20)", fieldname = "updatetime", comment = "")
    private String updatetime = ""; //创建时间

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE `db_clubroomcard` (\n" +
                "   `id` int(20) NOT NULL,\n" +
                "  `roomcard` int(20) DEFAULT NULL,\n" +
                "  `type` int(20) DEFAULT NULL,\n" +
                "  `clubID` int(20) DEFAULT NULL,\n" +
                "  `fromagentsID` int(20) DEFAULT NULL,\n" +
                "  `fromlevel` int(20) DEFAULT NULL,\n" +
                "  `from_pre_num` int(20) DEFAULT NULL,\n" +
                "  `from_now_num` int(20) DEFAULT NULL,\n" +
                "  `club_pre_num` int(20) DEFAULT NULL,\n" +
                "  `club_now_num` int(20) DEFAULT NULL,\n" +
                "  `updatetime` varchar(40) DEFAULT NULL,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
        return sql;
    }

}
