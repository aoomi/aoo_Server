package business.sdk;

import business.account.AccountManager;
import cenum.CommonFieldEnum;
import com.ddm.server.common.utils.AesEncryptUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import com.ddm.server.http.OKHttpUtil;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jsproto.c2s.cclass.user.UserAccessTokenInfo;
import jsproto.c2s.cclass.user.UserInfo;
import jsproto.c2s.cclass.user.UserValidityInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import server.aoo.dao.utils.ConfigUtils;
import java.util.Map;

/**
 * 电话登录（手机号登录）
 */
@Data
@Component
public class MobileManager {
    @Autowired
    private AccountManager accountManager;
    /**
     * 用户验证信息
     */
    private Map<String,UserValidityInfo> userValidityInfoMap = Maps.newConcurrentMap();

    /**
     * 请求应用程序的帐户有效性
     * @param receivePack 请求的包
     * @param mpID
     * @param code
     * @param sdkAccountID
     * @return
     */
    public UserInfo requestAccountValidityForApp(String receivePack,String mpID, String code, long sdkAccountID, int accountType) {
        if (sdkAccountID > 0L) {
            return this.requestAccountValidityByCache(mpID,code,sdkAccountID,accountType);
        }
        // 通过访问令牌获取用户信息
        return this.getUserInfo(this.getAccessToken(code,mpID),receivePack);
    }


    /**
     * 获取访问令牌
     * 手机验证码验证
     * @param code
     * @param appid
     * @return
     */
    public UserAccessTokenInfo getAccessToken(String code,String appid) {
        Map<String, String> params = Maps.newHashMap();
        params.put(CommonFieldEnum.MOBILE_ID.value(), appid);
        params.put(CommonFieldEnum.MOBILE_CODE.value(), code);
        String resultInfo =  OKHttpUtil.get(ConfigUtils.getMobileAccessTokenUrl(),params);
        if(StringUtils.isEmpty(resultInfo)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        JsonObject tokenInfo = JsonParser.parseString(resultInfo).getAsJsonObject();
        if(!tokenInfo.has(CommonFieldEnum.MOBILE_CODE.value())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        int errorCode =  tokenInfo.get(CommonFieldEnum.MOBILE_CODE.value()).getAsInt();
        if (errorCode <= -1) {
            // 验证码错误
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        String accessToken = AesEncryptUtils.encrypt(String.format("%s:%s",appid,code));
        if (StringUtils.isEmpty(accessToken)) {
            // 生成验证令牌错误
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        tokenInfo.addProperty(CommonFieldEnum.ACCESS_TOKEN.value(),accessToken);
        tokenInfo.addProperty(CommonFieldEnum.MOBILE_ID.value(),appid);
        tokenInfo.addProperty(CommonFieldEnum.UNIONID.value(),appid);

        Map<String, String> userInfoParams = Maps.newHashMap();
        userInfoParams.put(CommonFieldEnum.ACCESS_TOKEN.value(), accessToken);
        return UserAccessTokenInfo.Of(userInfoParams,tokenInfo );
    }

    /**
     * 通过访问令牌获取用户信息
     * @param userAccessTokenInfo 获取访问令牌和用户信息请求参数
     * @return
     */
    public UserInfo getUserInfo(UserAccessTokenInfo userAccessTokenInfo,String receivePack) {
        String unionid = userAccessTokenInfo.getTokenInfo().get(CommonFieldEnum.UNIONID.value()).getAsString();
        if (StringUtils.isEmpty(unionid)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 记录验证信息
        this.userValidityInfoMap.put(unionid,UserValidityInfo.Of(userAccessTokenInfo.getTokenInfo(),null ));
        return UserInfo.Of(unionid,0,"" ,"" ,userAccessTokenInfo.getUserInfoParams().get(CommonFieldEnum.ACCESS_TOKEN.value()) ,"" ,unionid ,"0");
    }

    /**
     * 通过缓存请求帐户有效性
     * @param appid
     * @param sdkToken 令牌
     * @param sdkAccountID 账号Id
     * @return
     */
    public UserInfo requestAccountValidityByCache(String appid,String sdkToken,long sdkAccountID,int accountType) {
        String uid = this.accountManager.getAccountIDByCharAccount(sdkAccountID,accountType);
        if (!this.userValidityInfoMap.containsKey(uid)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 获取用户验证信息
        UserValidityInfo userValidityInfo = this.userValidityInfoMap.get(uid);
        if (userValidityInfo.existError()) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        if (StringUtils.isEmpty(sdkToken) || !sdkToken.equals(userValidityInfo.getTokenInfo().get(CommonFieldEnum.ACCESS_TOKEN.value()).getAsString())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 唯一Id
        String unionid = userValidityInfo.getTokenInfo().get(CommonFieldEnum.UNIONID.value()).getAsString();
        return UserInfo.Of(unionid,0,"" ,"" ,sdkToken,"" ,unionid,"12313");
    }
}
