package business.global.client;

import business.account.Account;
import business.account.AccountManager;
import business.global.strategy.AbstractPackStrategy;
import business.sdk.MobileManager;
import business.sdk.WeChatAppManager;
import business.sdk.WeChatManager;
import cenum.sdk.SDKTypeEnum;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import jsproto.c2s.cclass.user.UserInfo;
import jsproto.c2s.iclass.client.CFF02_LoginAccountBySDK;
import jsproto.c2s.iclass.client.S0000_AccountLogin;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.aoo.dao.entity.mongo.DbTagAccountInfo;

import java.util.Objects;

@Data
@Service("CFF02_LoginAccountBySDK")
public class FF02_LoginAccountBySDK extends AbstractPackStrategy {
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private WeChatAppManager weChatAppManager;
    @Autowired
    private MobileManager mobileManager;
    @Autowired
    private WeChatManager weChatManager;
    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        CFF02_LoginAccountBySDK loginAccountBySDK = GsonUtils.stringToBean(receivePack, CFF02_LoginAccountBySDK.class);
        if (loginAccountBySDK.getSDKType() == SDKTypeEnum.SDKType_WeChat.getValue()) {
            // 请求账号合法性
            return onRequestAccountValidity(loginAccountBySDK.getSDKType(),senderInfo,this.weChatManager.requestAccountValidity(loginAccountBySDK.getUserData(), loginAccountBySDK.getToken()));
        }else if (loginAccountBySDK.getSDKType() == SDKTypeEnum.SDKType_WeChatApp.getValue()) {
            // 微信请求应用程序的帐户有效性
            return onRequestAccountValidity(loginAccountBySDK.getSDKType(),senderInfo,this.weChatAppManager.requestAccountValidityForApp(loginAccountBySDK.getUserData(), loginAccountBySDK.getToken(), loginAccountBySDK.getSDKAccountID(),loginAccountBySDK.getSDKType()));
        } else if (loginAccountBySDK.getSDKType() == SDKTypeEnum.SDKType_Mobile.getValue()) {
            // 手机登录
            return onRequestAccountValidity(loginAccountBySDK.getSDKType(),senderInfo,this.mobileManager.requestAccountValidityForApp(receivePack,loginAccountBySDK.getUserData(), loginAccountBySDK.getToken(), loginAccountBySDK.getSDKAccountID(),loginAccountBySDK.getSDKType()));
        }
        throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
    }

    /**
     * 请求账号验证
     * @param sdkType sdk类型
     * @param senderIP 发送ip
     * @param userInfo 用户信息
     * @return
     */
    public String onRequestAccountValidity(int sdkType,String senderIP,UserInfo userInfo) {
        if(StringUtils.isEmpty(userInfo.getUid())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        S0000_AccountLogin accountLogin = null;
        long accountId = this.accountManager.getAccountIDByCharAccount(sdkType,userInfo.getUid());
        if (accountId > 0L) {
            accountLogin = this.accountManager.getAccountLoginSendPack(accountId,sdkType);
        } else {
            accountLogin = this.accountManager.createAccountByCharAccount(userInfo.getUid(),userInfo.getCharAccountPsw(),sdkType,senderIP);
        }
        accountLogin.setNickName(userInfo.getNickName());
        accountLogin.setSex(userInfo.getSex());
        accountLogin.setSDKToken(userInfo.getToken());
        accountLogin.setHeadImageUrl(userInfo.getHeadImageUrl());
        accountLogin.setOpenid(userInfo.getOpenid());
        accountLogin.setUnionid(userInfo.getUnionid());
        return GsonUtils.toJsonString(accountLogin);
    }


}
