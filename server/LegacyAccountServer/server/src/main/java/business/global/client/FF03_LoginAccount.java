package business.global.client;

import business.account.Account;
import business.account.AccountManager;
import business.global.strategy.AbstractPackStrategy;
import cenum.DefaultEnum;
import cenum.sdk.SDKTypeEnum;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import jsproto.c2s.cclass.token.CreateAccountTokenInfo;
import jsproto.c2s.iclass.client.CFF03_LoginAccount;
import jsproto.c2s.iclass.client.S0000_AccountLogin;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("CFF03_LoginAccount")
public class FF03_LoginAccount extends AbstractPackStrategy {
    @Autowired
    private AccountManager accountManager;


    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        CFF03_LoginAccount loginAccount = GsonUtils.stringToBean(receivePack, CFF03_LoginAccount.class);
        if (StringUtils.isEmpty(loginAccount.getCharAccountPsw()) || loginAccount.getCharAccountPsw().length() < 6) {
            // 密码不能为空
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultCode(),CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultMsg());
        }
        // 获取账号id
        long accountId = this.accountManager.getAccountIDByCharAccount(SDKTypeEnum.SDKType_Company.getValue(),loginAccount.getCharAccount());
        if (accountId <= 0L) {
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultCode(),CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultMsg());
        }
        CreateAccountTokenInfo accountTokenInfo = null;
        if (loginAccount.getIsToken() >= DefaultEnum.EXIST.value()) {
            // 解析token
            accountTokenInfo = this.accountManager.parseAccountToken(loginAccount.getCharAccountPsw());
            if (Objects.isNull(accountTokenInfo) || accountId != accountTokenInfo.getAccountID()) {
                throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTTOKENERROR.getResultCode(),CommonEnum.KICKOUT_ACCOUNTTOKENERROR.getResultMsg());
            }
        }
        // 获取账号信息
        Account account = this.accountManager.getPlayerLogin(accountId, SDKTypeEnum.SDKType_Company.getValue());
        if (Objects.isNull(account)) {
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultCode(),CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultMsg());
        }
        return GsonUtils.toJsonString(this.checkLoginAccountVerification(accountId,loginAccount.getCharAccountPsw(),account,accountTokenInfo));
    }

    /**
     * 检查登录账号的验证
     */
    public S0000_AccountLogin checkLoginAccountVerification(long accountId, String CharAccountPsw, Account account, CreateAccountTokenInfo accountTokenInfo) {
        String accountPsw = account.getAccountTypeToCharAccountPsw(SDKTypeEnum.SDKType_Company.getValue());
        if (Objects.isNull(accountTokenInfo)) {
            if (!business.security.PasswordHasher.verify(CharAccountPsw, accountPsw)) {
                throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultCode(),CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultMsg());
            }
            if (!business.security.PasswordHasher.isEncoded(accountPsw)) {
                accountPsw = business.security.PasswordHasher.hash(CharAccountPsw);
                account.saveAccountPasswordHash(SDKTypeEnum.SDKType_Company.getValue(), accountPsw);
            }
        } else {
            if (!business.security.PasswordHasher.constantTimeEquals(
                    this.accountManager.tokenCredential(accountPsw), accountTokenInfo.getPsw())) {
                throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultCode(),CommonEnum.KICKOUT_ACCOUNTPSWERROR.getResultMsg());
            }
        }
        // 获取指定账号的信息
        return this.accountManager.getAccountLoginSendPack(accountId,SDKTypeEnum.SDKType_Company.getValue());
    }

}
