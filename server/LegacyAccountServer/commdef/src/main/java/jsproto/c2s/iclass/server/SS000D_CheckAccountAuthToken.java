package jsproto.c2s.iclass.server;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SS000D_CheckAccountAuthToken extends BaseSendMsg {
    /**
     * 返回码
     */
    private int Code;
    /**
     * 账号id
     */
    private long AccountID;
    /**
     * 账号类型
     */
    private int AccountType;
    /**
     * 账号
     */
    private String CharAccount;
    /**
     * 服务端id
     */
    private int ServerID = 1;


    public static SS000D_CheckAccountAuthToken make(int code, long accountID, int AccountType, String CharAccount) {
        SS000D_CheckAccountAuthToken accountAuthToken = new SS000D_CheckAccountAuthToken();
        accountAuthToken.setCode(code);
        accountAuthToken.setAccountID(accountID);
        accountAuthToken.setAccountType(AccountType);
        accountAuthToken.setCharAccount(CharAccount);
        return accountAuthToken;
    }
}
