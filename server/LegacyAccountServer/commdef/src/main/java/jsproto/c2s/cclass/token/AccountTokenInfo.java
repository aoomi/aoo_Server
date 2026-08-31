package jsproto.c2s.cclass.token;

import com.ddm.server.common.utils.GsonUtils;
import lombok.Data;

/**
 * 账号的token信息
 */
@Data
public class AccountTokenInfo {
    /**
     * 生成的token信息
     */
    private String token;
    /**
     * 密码
     */
    private int accountType;
    /**
     * 手机
     */
    private String chatAccount;

    /**
     * 密码
     */
    private String psw;


    public AccountTokenInfo(String token, int accountType, String chatAccount,String psw) {
        this.token = token;
        this.accountType = accountType;
        this.chatAccount = chatAccount;
        this.psw = psw;
    }


    public final static AccountTokenInfo Of(String token, int accountType, String chatAccount,String psw) {
        return new AccountTokenInfo(token,accountType,chatAccount,psw);
    }
}
