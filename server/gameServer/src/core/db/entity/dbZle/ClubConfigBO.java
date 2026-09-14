//package core.db.entity.dbZle;
//
//import com.ddm.server.annotation.DataBaseField;
//import com.ddm.server.annotation.TableName;
//import core.db.entity.BaseEntity;
//import lombok.Data;
//
//
//@TableName(value = "db_clubconfig")
//@Data
//public class ClubConfigBO extends BaseEntity<ClubConfigBO> {
//
//    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "",indextype = DataBaseField.IndexType.Unique)
//	private long id;
//    @DataBaseField(type = "bigint(20)", fieldname = "numforagents", comment = "")
//	private int numforagents; //每个代理能开多少个俱乐部
//    @DataBaseField(type = "bigint(20)", fieldname = "numforplayer", comment = "")
//	private int numforplayer;//每个玩家能加入多少个俱乐部
//    @DataBaseField(type = "bigint(20)", fieldname = "ministernum", comment = "")
//	private int ministernum;//每个俱乐部最多允许几个管理员
//    @DataBaseField(type = "bigint(20)", fieldname = "clubIDlenth", comment = "")
//	private int clubIDlenth;//俱乐部标识ID的长度
//    @DataBaseField(type = "bigint(20)", fieldname = "leastcard", comment = "")
//	private int leastcard; //代理开启俱乐部的最低房卡限制(暂时没用)
//    @DataBaseField(type = "bigint(20)", fieldname = "agentsID", comment = "")
//	private int agentsID;//代理ID(暂时没用)
//    @DataBaseField(type = "bigint(20)", fieldname = "level", comment = "")
//    private int level = 10001;//代理等级(暂时没用)
//    @DataBaseField(type = "bigint(20)", fieldname = "maxplayernum", comment = "")
//    private int maxplayernum;//俱乐部最高人数上限
//    @DataBaseField(type = "bigint(20)", fieldname = "rewardRoomCard", comment = "")
//    private int rewardRoomCard;//俱乐部奖励房卡
//    @DataBaseField(type = "bigint(20)", fieldname = "createGameSetMax", comment = "")
//    private int createGameSetMax;//一个俱乐部最大可以设置几个自动房间
//
//    public static String getCreateTableSQL() {
//        String sql = "CREATE TABLE `db_clubconfig` (\n" +
//                "   `id` int(20) NOT NULL,\n" +
//                "  `numforagents` int(20) DEFAULT NULL,\n" +
//                "  `numforplayer` int(20) DEFAULT NULL,\n" +
//                "  `ministernum` int(20) DEFAULT NULL,\n" +
//                "  `clubIDlenth` int(20) DEFAULT NULL,\n" +
//                "  `leastcard` int(20) DEFAULT NULL,\n" +
//                "  `agentsID` int(20) DEFAULT NULL,\n" +
//                "  `level` int(20) DEFAULT NULL,\n" +
//                "  `maxplayernum` int(20) DEFAULT NULL,\n" +
//                "  `rewardRoomCard` int(20) DEFAULT NULL,\n" +
//                "  `createGameSetMax` int(20) DEFAULT NULL,\n" +
//                "    PRIMARY KEY (`id`)\n" +
//                ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
//        return sql;
//    }
//
//}
