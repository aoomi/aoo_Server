package jsproto.c2s.iclass.client;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 账号登录操作
 */
@Data
public class CFF03_LoginAccount extends BaseSendMsg {
    /**
     * 账号
     */
    private String CharAccount;
    /**
     * 密码
     */
    private String CharAccountPsw;
    /**
     * 是否验证token
     */
    private int IsToken;


    @Override
    public String toString() {
        return "CFF03_LoginAccount{" +
                "CharAccount='" + CharAccount + '\'' +
                ", CharAccountPsw='" + CharAccountPsw + '\'' +
                ", IsToken=" + IsToken +
                ", Head=" + Head +
                '}';
    }
}
