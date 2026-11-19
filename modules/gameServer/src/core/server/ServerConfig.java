package core.server;

import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommString;
import com.ddm.server.common.utils.CommTime;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig extends Config {

    private static List<Integer> serverMergedIds = new ArrayList<Integer>();

    static {
        serverMergedIds.addAll(CommString.getIntegerList(System.getProperty("SERVER_MIDList", "" + ServerID()), ";"));
    }

    public static List<Integer> SERVER_MIDList() {
        return serverMergedIds;
    }

    public static int AAY_AppId() {
        return 10294;
    }

    public static String AAY_SecretKey() {
        return System.getProperty("AAY_SecretKey", "2767c8cf315a4e8ebd50e5f9bb52fd3a");
    }

    private static int loginkey = CommTime.nowSecond();

    public static int refreshLoginKey() {
        return loginkey = CommTime.nowSecond();
    }

    public static int getLoginKey() {
        return loginkey;
    }

}
