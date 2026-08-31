package business.global.secret;

import cenum.redis.RedisBydrKeyEnum;
import core.ioc.ContainerMgr;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家管理
 *
 * @author Hxing
 */
@Data
public class SecretManager {
    /**
     * 服务端
     */
    private static final String SERVER_TOKEN = "SERVER_TOKEN";
    /**
     * 客户端
     */
    private static final String CLIENT_TOKEN = "CLIENT_TOKEN";


    /**
     * 重新登录token信息
     *
     * @param accountId
     * @param clientToken
     * @param serverToken
     */
    public static final void saveC1004Login(long accountId, String clientToken, String serverToken) {
        Map<String, String> map = new HashMap<>();
        map.put(CLIENT_TOKEN, clientToken);
        map.put(SERVER_TOKEN, serverToken);
        ContainerMgr.get().getRedis().getMap(RedisBydrKeyEnum.AID_2_TOKEN.getKey(accountId)).putAll(map);
    }

    /**
     * 断线重连token信息
     *
     * @param accountId
     * @param serverToken
     */
    public static final void saveC1009ResetLogin(long accountId, String serverToken) {
        ContainerMgr.get().getRedis().getMap(RedisBydrKeyEnum.AID_2_TOKEN.getKey(accountId)).put(SERVER_TOKEN, serverToken);
    }

}
