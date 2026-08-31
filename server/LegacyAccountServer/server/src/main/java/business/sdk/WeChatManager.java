package business.sdk;

import business.account.AccountManager;
import cenum.CommonFieldEnum;
import cenum.SexEnum;
import com.ddm.server.common.utils.CommLog;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import com.ddm.server.http.OKHttpUtil;
import com.ddm.server.http.StringCallback;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import jsproto.c2s.cclass.general.TokenInfo;
import jsproto.c2s.cclass.user.UserAccessTokenInfo;
import jsproto.c2s.cclass.user.UserInfo;
import jsproto.c2s.cclass.user.UserValidityInfo;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import server.aoo.dao.utils.ConfigUtils;

import java.util.*;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家管理
 *
 * @author Hxing
 */
@Data
@Component
public class WeChatManager {

    @Autowired
    private AccountManager accountManager;
    /**
     * 用户验证信息
     */
    private Map<String,UserValidityInfo> userValidityInfoMap = Maps.newConcurrentMap();
    /**
     * mp访问令牌
     */
    private Map<String,JsonObject> mpAccessTokenMap = Maps.newConcurrentMap();

    /**
     * 初始化所有微信公众号token
     */
    public void onServerInitOK() {
        // 请求微信授权信息
        this.requestWeChatAccessTokenInfo();
    }

    /**
     * 请求账号合法性
     * @param mpID
     * @param code
     * @return
     */
    public UserInfo  requestAccountValidity(String mpID, String code) {
        // mp访问令牌
        JsonObject mpAccessToken = this.getWeChatAccessTokenInfo().get(mpID);
        if (Objects.isNull(mpAccessToken)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 获取appid
        String appid = mpAccessToken.get(CommonFieldEnum.WX_APPID.value()).getAsString();
        // 获取秘钥
        String secret = mpAccessToken.get(CommonFieldEnum.WX_SECRET.value()).getAsString();
        // 通过访问令牌获取用户信息
        return this.getUserInfo(this.getAccessToken(code,appid,secret));
    }






    /**
     * 获取访问令牌
     * @return
     */
    public UserAccessTokenInfo getAccessToken(String code,String appid,String secret) {
        Map<String, String> params = Maps.newHashMap();
        params.put(CommonFieldEnum.WX_APPID.value(), appid);
        params.put(CommonFieldEnum.WX_SECRET.value(), secret);
        params.put(CommonFieldEnum.WX_CODE.value(), code);
        params.put(CommonFieldEnum.GRANT_TYPE.value(), ConfigUtils.getWxGrantType());
        String resultInfo =  OKHttpUtil.get(ConfigUtils.getWxAccessTokenUrl(),params);
        if(StringUtils.isEmpty(resultInfo)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        JsonObject tokenInfo = JsonParser.parseString(resultInfo).getAsJsonObject();
        if(tokenInfo.has(CommonFieldEnum.ERRCODE.value())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        //获取map里面的内容
        String accessToken = tokenInfo.get(CommonFieldEnum.ACCESS_TOKEN.value()).getAsString();
        String openid = tokenInfo.get(CommonFieldEnum.OPENID.value()).getAsString();
        if (!tokenInfo.has(CommonFieldEnum.UNIONID.value())) {
            tokenInfo.addProperty(CommonFieldEnum.UNIONID.value(),openid );
        }
        String unionid = tokenInfo.get(CommonFieldEnum.UNIONID.value()).getAsString();
        if(StringUtils.isEmpty(unionid)){
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        tokenInfo.addProperty(CommonFieldEnum.WX_APPID.value(),appid );
        tokenInfo.addProperty(CommonFieldEnum.WX_SECRET.value(), secret);

        Map<String, String> userInfoParams = Maps.newHashMap();
        userInfoParams.put(CommonFieldEnum.ACCESS_TOKEN.value(), accessToken);
        userInfoParams.put(CommonFieldEnum.OPENID.value(), openid);
        userInfoParams.put(CommonFieldEnum.LANG.value(), ConfigUtils.getWxLangType());
        return UserAccessTokenInfo.Of(userInfoParams,tokenInfo );
    }

    /**
     * 通过访问令牌获取用户信息
     * @param userAccessTokenInfo 获取访问令牌和用户信息请求参数
     * @return
     */
    public UserInfo getUserInfo(UserAccessTokenInfo userAccessTokenInfo) {
        String content = OKHttpUtil.get(ConfigUtils.getWxAccountUserInfoUrl(), userAccessTokenInfo.getUserInfoParams());
        if(StringUtils.isEmpty(content)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        JsonObject playerInfo = JsonParser.parseString(content).getAsJsonObject();
        if(playerInfo.has(CommonFieldEnum.ERRCODE.value())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        String openid = playerInfo.get(CommonFieldEnum.OPENID.value()).getAsString();
        if (StringUtils.isEmpty(openid)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        if (!playerInfo.has(CommonFieldEnum.UNIONID.value())) {
            playerInfo.addProperty(CommonFieldEnum.UNIONID.value(),openid);
        }
       String unionid = playerInfo.get(CommonFieldEnum.UNIONID.value()).getAsString();
        if (StringUtils.isEmpty(unionid)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 记录验证信息
        this.userValidityInfoMap.put(unionid,UserValidityInfo.Of(userAccessTokenInfo.getTokenInfo(),playerInfo ));
        // 性别
        int sex = playerInfo.get(CommonFieldEnum.SEX.value()).getAsInt() == 2  ? SexEnum.HeroSex_Girl.ordinal(): SexEnum.HeroSex_Boy.ordinal();
        // 名称
        String nickName = playerInfo.get(CommonFieldEnum.NICK_NAME.value()).getAsString();
        // 头像
        String headImageUrl = playerInfo.get(CommonFieldEnum.HEAD_IMG_URL.value()).getAsString();
        return UserInfo.Of(unionid,sex,nickName ,headImageUrl ,userAccessTokenInfo.getUserInfoParams().get(CommonFieldEnum.ACCESS_TOKEN.value()) ,userAccessTokenInfo.getUserInfoParams().get(CommonFieldEnum.OPENID.value()) ,unionid,"0");
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
        String unionid = userValidityInfo.getPlayerInfo().get(CommonFieldEnum.UNIONID.value()).getAsString();
        // 开放Id
        String openid = userValidityInfo.getPlayerInfo().get(CommonFieldEnum.OPENID.value()).getAsString();
        // 性别
        int sex = userValidityInfo.getPlayerInfo().get(CommonFieldEnum.SEX.value()).getAsInt() == 2  ? SexEnum.HeroSex_Girl.ordinal(): SexEnum.HeroSex_Boy.ordinal();
        // 名称
        String nickName = userValidityInfo.getPlayerInfo().get(CommonFieldEnum.NICK_NAME.value()).getAsString();
        // 头像
        String headImageUrl = userValidityInfo.getPlayerInfo().get(CommonFieldEnum.HEAD_IMG_URL.value()).getAsString();
        return UserInfo.Of(unionid,sex,nickName ,headImageUrl ,sdkToken,openid ,unionid,"0");
    }


    /**
     * 请求微信授权信息
     */
    public void requestWeChatAccessTokenInfo() {
        Map<String, String> userInfoParams = Maps.newHashMap();
        userInfoParams.put(CommonFieldEnum.HEAD.value(), "7");
        String content = OKHttpUtil.post(ConfigUtils.getWeChatServerUrl(), userInfoParams);
        if (StringUtils.isEmpty(content)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        this.mpAccessTokenMap = GsonUtils.stringToBean(content, new TypeToken<Map<String, JsonObject>>() {}.getType());
        CommLog.info("初始化所有微信公众号token");
    }



    public Map<String, JsonObject> getWeChatAccessTokenInfo() {
        return MapUtils.isEmpty(this.mpAccessTokenMap) ? Collections.emptyMap() :this.mpAccessTokenMap;
    }


    public void onHalfHour() {
        // 当前时间
        long nowTick = CommTime.nowMS();
        for (Map.Entry<String, UserValidityInfo> entrySet :this.userValidityInfoMap.entrySet()) {
            JsonObject tokenInfo = entrySet.getValue().getTokenInfo();
            if(Objects.isNull(tokenInfo)) {
                this.userValidityInfoMap.remove(entrySet.getKey());
                continue;
            }
            if (nowTick < tokenInfo.get(CommonFieldEnum.END_TICK.value()).getAsLong()) {
                continue;
            }
            String appID = tokenInfo.get(CommonFieldEnum.WX_APPID.value()).getAsString();
            String refreshToken = tokenInfo.get(CommonFieldEnum.REFRESH_TOKEN.value()).getAsString();

            Map<String, String> params = Maps.newHashMap();
            params.put(CommonFieldEnum.WX_APPID.value(), appID);
            params.put(CommonFieldEnum.GRANT_TYPE.value(), CommonFieldEnum.REFRESH_TOKEN.value());
            params.put(CommonFieldEnum.REFRESH_TOKEN.value(), refreshToken);
            OKHttpUtil.get(ConfigUtils.getWxRefreshTokenUrl(), params, new StringCallback() {
                @Override
                public void completed(String resultInfo) {
                    // 刷新token
                    onRefreshToken(entrySet.getKey(), resultInfo);
                }
                @Override
                public void failed(Exception ex) {
                    throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
                }
            });
        }
    }

    /**
     * 刷新token
     * @param uid
     * @param result
     */
    private void onRefreshToken(String uid,String result) {
        JsonObject resultInfo = JsonParser.parseString(result).getAsJsonObject();
        if(resultInfo.has(CommonFieldEnum.ERRCODE.value())) {
            this.userValidityInfoMap.remove(uid);
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 获取用户验证信息
        UserValidityInfo userValidityInfo = this.userValidityInfoMap.get(uid);
        if (Objects.isNull(userValidityInfo)) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        userValidityInfo.getTokenInfo().addProperty(CommonFieldEnum.END_TICK.value(), CommTime.nowMS() + CommMath.randomInt(resultInfo.get(CommonFieldEnum.EXPIRES_IN.value()).getAsInt() * 1000*2/3));
        userValidityInfo.getTokenInfo().addProperty(CommonFieldEnum.REFRESH_TOKEN.value(), resultInfo.get(CommonFieldEnum.REFRESH_TOKEN.value()).getAsString());
        userValidityInfo.getTokenInfo().addProperty(CommonFieldEnum.ACCESS_TOKEN.value(), resultInfo.get(CommonFieldEnum.ACCESS_TOKEN.value()).getAsString());
        this.userValidityInfoMap.put(uid,userValidityInfo );
    }


}
