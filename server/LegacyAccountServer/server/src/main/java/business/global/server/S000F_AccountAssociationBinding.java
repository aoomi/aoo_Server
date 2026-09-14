package business.global.server;

import business.account.Account;
import business.account.AccountManager;
import business.global.strategy.AbstractPackStrategy;
import business.sdk.MobileManager;
import business.sdk.WeChatAppManager;
import cenum.sdk.SDKTypeEnum;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import jsproto.c2s.cclass.user.UserInfo;
import jsproto.c2s.iclass.server.CS000F_AccountAssociationBinding;
import jsproto.c2s.iclass.server.SS000D_CheckAccountAuthToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.aoo.dao.service.mongo.TagAccountTypeService;

import java.util.Objects;

/**
 * 游戏中做授权绑定
 */
@Service("CS000F_AccountAssociationBinding")
public class S000F_AccountAssociationBinding extends AbstractPackStrategy {
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private WeChatAppManager weChatAppManager;
    @Autowired
    private MobileManager mobileManager;


    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        CS000F_AccountAssociationBinding accountAssociationBinding = GsonUtils.stringToBean(receivePack, CS000F_AccountAssociationBinding.class);
        Account account = this.accountManager.getPlayerLogin(accountAssociationBinding.getAccountId(),accountAssociationBinding.getSDKType() );
        if (Objects.isNull(account)) {
            // 找不到账号
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        String accountTypeId = TagAccountTypeService.getId(accountAssociationBinding.getSDKType(), account.getAccountId());
        if(account.getTagAccountTypeMap().containsKey(accountTypeId)) {
            // 该关联类型账号已绑定
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        if (accountAssociationBinding.getSDKType() == SDKTypeEnum.SDKType_WeChatApp.getValue()) {
            // 微信请求应用程序的帐户有效性
            return onRequestAccountAssociationBinding(account,accountAssociationBinding.getSDKType(),senderInfo,this.weChatAppManager.requestAccountValidityForApp(accountAssociationBinding.getUserData(), accountAssociationBinding.getToken(), 0L,accountAssociationBinding.getSDKType()));
        } else if (accountAssociationBinding.getSDKType() == SDKTypeEnum.SDKType_Mobile.getValue()) {
            // 手机登录
            return onRequestAccountAssociationBinding(account,accountAssociationBinding.getSDKType(),senderInfo,this.mobileManager.requestAccountValidityForApp(receivePack,accountAssociationBinding.getUserData(), accountAssociationBinding.getToken(), 0L,accountAssociationBinding.getSDKType()));
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
    public String onRequestAccountAssociationBinding(Account account,int sdkType,String senderIP,UserInfo userInfo) {
        if(StringUtils.isEmpty(userInfo.getUid())) {
            throw BizException.Of(CommonEnum.HTTP_PACKNOTACTION.getResultCode(),CommonEnum.HTTP_PACKNOTACTION.getResultMsg());
        }
        // 增加账号类型
        account.newAccountTypePut(sdkType,userInfo.getUid(), userInfo.getCharAccountPsw());
        return GsonUtils.toJsonString(SS000D_CheckAccountAuthToken.make(0, account.getAccountId(), sdkType,userInfo.getUid()));
    }
}
