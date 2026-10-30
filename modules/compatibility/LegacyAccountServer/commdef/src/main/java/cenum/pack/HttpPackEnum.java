package cenum.pack;

/**
 * 客户端发送给服务器的封包
 */
public enum HttpPackEnum {
    /**
     * 空
     */
    NOT(0,""),
    /**
     * 一键注册
     */
    CLIENT_0xFF00_OneKeyRegAccount(0xff00,"CFF00_OneKeyRegAccount"),
    /**
     * 登录账号SDK
     */
    CLIENT_0xFF02_LoginAccountBySDK(0xff02,"CFF02_LoginAccountBySDK"),
    /**
     * 登录账号
     */
    CLIENT_0xFF03_LoginAccount(0xff03,"CFF03_LoginAccount"),
    /**
     * 注册角色
     */
    CLIENT_0xFF07_RegAccount(0xff07,"CFF07_RegAccount"),
    /**
     * 修改密码
     */
    CLIENT_0xFF08_ChangeAccountPsw(0xff08,"CFF08_ChangeAccountPsw"),
    /**
     * 忘记密码
     */
    CLIENT_0xFF0B_ForgetAccountPsw(0xff0B,"CFF0B_ForgetAccountPsw"),

    /**
     * 验证
     */
    SERVER_S000D_CheckAccountAuthToken(0x000D,"CS000D_CheckAccountAuthToken"),
    /**
     * 账号的关联绑定
     */
    SERVER_S000F_AccountAssociationBinding(0x000F,"CS000F_AccountAssociationBinding"),
    /**
     * 验证
     */
    SERVER_S0001_CheckAccountAuthToken(0x0001,"CS0001_CheckAccountAuthToken"),

    ;
    /**
     * 值
     */
    private int value;
    /**
     * 接口名称
     */
    private String iclassName;


    HttpPackEnum(int value, String iclassName) {
        this.value = value;
        this.iclassName = iclassName;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getIclassName() {
        return iclassName;
    }

    public void setIclassName(String iclassName) {
        this.iclassName = iclassName;
    }

    public static HttpPackEnum valueOf(int parkId) {
        for (HttpPackEnum parkEnum : HttpPackEnum.values()) {
            if (parkEnum.value == parkId) {
                return parkEnum;
            }
        }
        return HttpPackEnum.NOT;
    }

}
