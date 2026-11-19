package jsproto.c2s.iclass.client;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 账号登录操作
 */
@Data
public class CFF08_ChangeAccountPsw extends BaseSendMsg {
    /**
     * 账号
     */
    private long CharAccount;
    /**
     * 密码
     */
    private String CharAccountPsw;
    /**
     * 是否验证token
     */
    private int IsToken;



}
