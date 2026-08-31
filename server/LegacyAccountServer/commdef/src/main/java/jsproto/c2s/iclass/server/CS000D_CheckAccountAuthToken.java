package jsproto.c2s.iclass.server;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 验证账号令牌是否正确
 */
@Data
public class CS000D_CheckAccountAuthToken extends BaseSendMsg {
    /**
     * 账号id
     */
    private long AccountID;
    /**
     * token信息
     */
    private String Token;
}
