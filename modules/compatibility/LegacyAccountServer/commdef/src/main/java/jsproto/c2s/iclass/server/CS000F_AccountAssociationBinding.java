package jsproto.c2s.iclass.server;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 账号关联绑定
 */
@Data
public class CS000F_AccountAssociationBinding extends BaseSendMsg {
    /**
     * 账号id
     */
    private long accountId;
    /**
     * 类型
     */
    private int SDKType;
    /**
     * 用户信息
     */
    private String UserData;
    /**
     * 令牌
     */
    private String Token;

}
