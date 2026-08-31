package business.global.client;

import business.global.strategy.AbstractPackStrategy;
import com.ddm.server.common.utils.GsonUtils;
import org.springframework.stereotype.Service;

@Service("CFF08_ChangeAccountPsw")
public class FF08_ChangeAccountPsw extends AbstractPackStrategy {
    @Override
    public String OnReceivePack(String senderInfo, String receivePack) {
        FF08_ChangeAccountPsw forgetAccountPsw = GsonUtils.stringToBean(receivePack, FF08_ChangeAccountPsw.class);
        return forgetAccountPsw.toString();
    }
}
