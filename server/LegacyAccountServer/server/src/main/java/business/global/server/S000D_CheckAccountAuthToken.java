package business.global.server;

import business.account.Account;
import business.account.AccountManager;
import business. global.strategy.AbstractPackStrategy;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import jsproto.c2s.cclass.token.AccountTokenInfo;
import jsproto.c2s.cclass.token.CreateAccountTokenInfo;
import jsproto.c2s.iclass.server.CS000D_CheckAccountAuthToken;
import jsproto.c2s.iclass.server.SS000D_CheckAccountAuthToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;


/**
 * 用户信息验证
 */
@Service("CS000D_CheckAccountAuthToken")
public class S000D_CheckAccountAuthToken extends AbstractPackStrategy {
    @Autowired
    private AccountManager accountManager;
    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        CS000D_CheckAccountAuthToken checkAccountAuthToken = GsonUtils.stringToBean(receivePack, CS000D_CheckAccountAuthToken.class);
        CreateAccountTokenInfo createAccountTokenInfo = this.accountManager.parseAccountToken(checkAccountAuthToken.getToken());
        Account account = this.accountManager.getPlayerLogin(checkAccountAuthToken.getAccountID(), createAccountTokenInfo.getAccountType());
        if (Objects.isNull(account)) {
            // 找不到账号
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultCode(),CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultMsg());
        }
        // 获取指定账号的token信息
        AccountTokenInfo accountTokenInfo = account.getAccountTokenInfo();
        if (Objects.nonNull(accountTokenInfo)
                && (StringUtils.isEmpty(accountTokenInfo.getToken()) || !accountTokenInfo.getToken().equals(checkAccountAuthToken.getToken()))) {
            // token 不匹配
            throw BizException.Of(CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultCode(),CommonEnum.TOKEN_VALIDATION_FAILED_ERROR.getResultMsg());
        }
        if (createAccountTokenInfo.getAccountID() != checkAccountAuthToken.getAccountID()) {
            // 账号id
            throw BizException.Of(CommonEnum.TOKEN_ACCOUNT_ID_ERROR.getResultCode(),CommonEnum.TOKEN_ACCOUNT_ID_ERROR.getResultMsg());
        }
        if (Objects.nonNull(accountTokenInfo) && createAccountTokenInfo.getAccountType() != accountTokenInfo.getAccountType()) {
            // 账号类型不对
            throw BizException.Of(CommonEnum.TOKEN_ACCOUNT_TYPE_ERROR.getResultCode(),CommonEnum.TOKEN_ACCOUNT_TYPE_ERROR.getResultMsg());
        }
        String storedCredential = this.accountManager.tokenCredential(
                account.getAccountTypeToCharAccountPsw(createAccountTokenInfo.getAccountType()));
        if (!createAccountTokenInfo.getPsw().equals(storedCredential)) {
            // 密码已经修改
            throw BizException.Of(CommonEnum.KICKOUT_NOTCREATETOKEN.getResultCode(),CommonEnum.KICKOUT_NOTCREATETOKEN.getResultMsg());
        }
        if (CommTime.SecondsBetween(createAccountTokenInfo.getCreateTick(),CommTime.nowMS()) >= CommTime.MinSec) {
            // token 超时
            throw BizException.Of(CommonEnum.KICKOUT_TOKENEXPIRE.getResultCode(),CommonEnum.KICKOUT_TOKENEXPIRE.getResultMsg());
        }
        account.refreshTime();
        return GsonUtils.toJsonString(SS000D_CheckAccountAuthToken.make(0, checkAccountAuthToken.getAccountID(), createAccountTokenInfo.getAccountType(),createAccountTokenInfo.getCharAccount()));
    }



}
