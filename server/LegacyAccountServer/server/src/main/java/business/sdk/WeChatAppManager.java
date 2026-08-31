package business.sdk;

import cenum.CommonFieldEnum;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import com.google.gson.JsonObject;
import jsproto.c2s.cclass.user.UserInfo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家管理
 *
 * @author Hxing
 */
@Data
@Component
public class WeChatAppManager {

    @Autowired
    private WeChatManager weChatManager;



    /**
     * 请求应用程序的帐户有效性
     * @param mpID
     * @param code
     * @param sdkAccountID
     * @return
     */
    public UserInfo  requestAccountValidityForApp(String mpID, String code, long sdkAccountID,int accountType) {
        // mp访问令牌
        JsonObject mpAccessToken = this.weChatManager.getWeChatAccessTokenInfo().get(mpID);
        if (Objects.isNull(mpAccessToken)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 获取appid
        String appid = mpAccessToken.get(CommonFieldEnum.WX_APPID.value()).getAsString();
        // 获取秘钥
        String secret = mpAccessToken.get(CommonFieldEnum.WX_SECRET.value()).getAsString();
        if (sdkAccountID > 0L) {
            return this.weChatManager.requestAccountValidityByCache(appid,code,sdkAccountID,accountType);
        }
        // 通过访问令牌获取用户信息
        return this.weChatManager.getUserInfo(this.weChatManager.getAccessToken(code,appid,secret));
    }
}
