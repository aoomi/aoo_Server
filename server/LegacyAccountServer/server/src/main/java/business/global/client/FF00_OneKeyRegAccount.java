package business.global.client;

import business.account.AccountManager;
import business.global.strategy.AbstractPackStrategy;
import com.ddm.server.common.utils.GsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("CFF00_OneKeyRegAccount")
public class FF00_OneKeyRegAccount extends AbstractPackStrategy {
    @Autowired
    private AccountManager accountManager;

    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        return GsonUtils.toJsonString(this.accountManager.createAccountBySeqKey(senderInfo));
    }
}
