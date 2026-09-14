package core.network.http.test;

import com.ddm.server.http.client.HttpUtil;

/**
 * @author xsj
 * @date 2020/9/24 11:14
 * @description 测试本地接口
 */
public class TestLocal {
    public static void kickClubMember() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("exePid", 274021);
//        map.put("playerID", 274028);
//        map.put("clubID", 708877);
//        map.put("status", 8);
//        map.put("sign", "33f77501874dbcd087ed565d9b511117");
//        String jsonData = new Gson().toJson(map);
        String data = "startTime=1604452218&endTime=1604452218&sign=3dede003b63290819535c0722c62744c";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/setMaintainServer", data, "");
    }

    public static void maintainServer() {
        String data = "type=2&sign=cd8a9a1b5e2354edbfad4bf06f14b03c";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/maintainServer", data, "");
    }

    public static void urgentmaintainServer() {
        String data = "nodeIp=127.0.0.1&nodePort=9996&sign=31eeb6cd51a471858163b07ba14cde3f";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/urgentMaintainServer", data, "");
    }

    public static void kickClubMember1() {

        String data = "startTime=1604452218&endTime=1604452218&sign=3dede003b63290819535c0722c62744c";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/setMaintainServer", data, "");
    }

    public static void setMaintainGame() {

        String data = "startTime=1604978434&endTime=1604982034&status=1&title=11sdfdf&content=11sdfsdf&gameTypeId=0&mainTitle=11sdfsdf&sign=c4f9e02822f7623ff1455b47e4d7a56d";
        String result = HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/setMaintainGame", data, "");
        System.out.println(result);
    }

    public static void getMaintainGame() {

        String data = "gameTypeId=7&sign=c8873bf0fff37e46aceab396e3897c16";
        String result = HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/getMaintainGame", data, "");
        System.out.println(result);
    }

    public static void listMaintainGame() {

        String data = "sign=96bdee2bc310b604fce1d09290eb72da";
        String result = HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/listMaintainGame", data, "");
        System.out.println(result);
    }

    public static void kickOutServer() {

        String data = "nodeIp=127.0.0.1&nodePort=9997&sign=3ebd512785d338e076dffb10503428be";
        String result = HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/kickOutServer", data, "");
        System.out.println(result);
    }

    public static void stopServer() {
        String data = "nodeIp=127.0.0.1&nodePort=9996&sign=31eeb6cd51a471858163b07ba14cde3f";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/stopServer", data, "");
    }

    public static void kickPlayer() {
        String data = "pid=379140&sign=abce9f7e0ead2e1321c1305d343c2dbf";
        HttpUtil.sendHttpPost2Web(3000, 300, "http://localhost:9888/kickPlayer", data, "");
    }

    public static void main(String[] args) {
        TestLocal.kickPlayer();
    }
}
