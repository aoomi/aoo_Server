package jsproto.c2s.cclass.user;

import com.google.gson.JsonObject;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 获取访问令牌和用户信息请求参数
 *
 */
@Data
public class UserAccessTokenInfo implements Serializable {

    /**
     * 获取用户信息请求参数
     */
    private Map<String, String> userInfoParams;
    /**
     * 令牌信息
     */
    private JsonObject tokenInfo;

    public UserAccessTokenInfo(Map<String, String> userInfoParams, JsonObject tokenInfo) {
        this.userInfoParams = userInfoParams;
        this.tokenInfo = tokenInfo;
    }


    public static final UserAccessTokenInfo Of(Map<String, String> userInfoParams, JsonObject tokenInfo) {
        return new UserAccessTokenInfo(userInfoParams,tokenInfo);
    }
}
