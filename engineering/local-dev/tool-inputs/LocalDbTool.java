import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** 本地闭环配置核对与测试数据维护工具。 */
public final class LocalDbTool {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/clark_game_new?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=5000&serverTimezone=Asia/Shanghai";
        try (Connection connection = DriverManager.getConnection(url, "root", "Quer_1234")) {
            if (args.length == 4 && "city-card".equals(args[0])) {
                long pid = Long.parseLong(args[1]);
                int cityId = Integer.parseInt(args[2]);
                int value = Integer.parseInt(args[3]);
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT id FROM playerCityCurrency WHERE pid=? AND cityId=? ORDER BY id LIMIT 1")) {
                    query.setLong(1, pid);
                    query.setInt(2, cityId);
                    long id = 0;
                    try (ResultSet rs = query.executeQuery()) {
                        if (rs.next()) id = rs.getLong(1);
                    }
                    if (id > 0) {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE playerCityCurrency SET value=?,time=UNIX_TIMESTAMP() WHERE pid=? AND cityId=?")) {
                            update.setInt(1, value);
                            update.setLong(2, pid);
                            update.setInt(3, cityId);
                            System.out.println("city-card-updated=" + update.executeUpdate());
                        }
                    } else {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO playerCityCurrency(pid,cityId,value,time) VALUES(?,?,?,UNIX_TIMESTAMP())")) {
                            insert.setLong(1, pid);
                            insert.setInt(2, cityId);
                            insert.setInt(3, value);
                            System.out.println("city-card-inserted=" + insert.executeUpdate());
                        }
                    }
                }
                return;
            }
            if (args.length == 3 && "club-city".equals(args[0])) {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE dbClubList SET cityId=? WHERE id=?")) {
                    update.setInt(1, Integer.parseInt(args[2]));
                    update.setLong(2, Long.parseLong(args[1]));
                    System.out.println("club-city-updated=" + update.executeUpdate());
                }
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT id,cityId FROM dbClubList WHERE id=?")) {
                    query.setLong(1, Long.parseLong(args[1]));
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) System.out.println("club-city=" + rs.getLong(1) + ":" + rs.getInt(2));
                    }
                }
                return;
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE gameType SET gameServerIP=?, gameServerPort=?, webSocketUrl=?, httpUrl=? WHERE gametype=?")) {
                update.setString(1, "127.0.0.1");
                update.setInt(2, 9997);
                update.setString(3, "ws://127.0.0.1:9996");
                update.setString(4, "http://127.0.0.1:9886");
                update.setInt(5, 629);
                System.out.println("updated=" + update.executeUpdate());
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT gametype,name,gameServerIP,gameServerPort,webSocketUrl,httpUrl FROM gameType WHERE gametype=?")) {
                query.setInt(1, 629);
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t" +
                                rs.getString(3) + ":" + rs.getInt(4) + "\t" + rs.getString(5) + "\t" + rs.getString(6));
                    }
                }
            }
        }
    }
}
