package jsproto.c2s.cclass.token;

import com.ddm.server.common.utils.GsonUtils;
import lombok.Data;

@Data
public class CreateAccountTokenInfo {
    /**
     * 账号Id
     */
    private long accountID;
    /**
     * 账号类型
     */
    private int accountType;
    /**
     * 账号
     */
    private String charAccount;
    /**
     * 创建时间
     */
    private long createTick;

    /**
     * 密码
     */
    private String psw;

    public CreateAccountTokenInfo() {
    }

    public CreateAccountTokenInfo(long accountID, int accountType, String charAccount, long createTick,String psw) {
        this.accountID = accountID;
        this.accountType = accountType;
        this.charAccount = charAccount;
        this.createTick = createTick;
        this.psw = psw;
    }

    public final static CreateAccountTokenInfo Of(long accountID,long createTick, int accountType, String charAccount,String psw) {
        return new CreateAccountTokenInfo(accountID,accountType,charAccount,createTick,psw);
    }



}
