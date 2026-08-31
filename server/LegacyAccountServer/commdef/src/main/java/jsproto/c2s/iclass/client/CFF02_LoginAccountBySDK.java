package jsproto.c2s.iclass.client;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CFF02_LoginAccountBySDK extends BaseSendMsg {
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
    /**
     * 账号id
     */
    private long SDKAccountID;
}
