package com.ddm.server.enums;


import com.ddm.server.exception.BaseErrorInfoInterface;

/**
 * 
 * @Title: CommonEnum
 * @Description: 公用描述枚举类
 * @Version:1.0.0
 * @author pancm
 * @date 2018年6月25日
 */
public enum CommonEnum implements BaseErrorInfoInterface {
	// 数据操作错误定义
	SUCCESS("0", "成功!"),
	NOT_FIND_PACK("1","封包未定义!"),
	PACK_RUN_ERROR("2","封包执行失败"),
	HERO_ID_NOT_FIND("3","玩家ID不存在"),
	PACK_NOT_ACTION("4","执行条件不允许"),
	RETURN_NOT_JSON("5","返回值非JSON"),
	RETURN_NOT_FIND_CODE("6","返回值未找到Code"),
	NOT_FIND_PACK_CLASS("7","封包PackClass未找到"),

	BODY_NOT_MATCH("400","请求的数据格式不符!"),
	SIGNATURE_NOT_MATCH("401","请求的数字签名不匹配!"),
	NOT_FOUND("404", "未找到该资源!"),
	INTERNAL_SERVER_ERROR("500", "服务器内部错误!"),
	SERVER_BUSY("503","服务器正忙，请稍后再试!"),

	PACKNOT_ACTION("10000","封包没有什么回复动作,或者条件没有通过不执行"),
	PACKRUN_ERROR("10001","封包执行崩溃"),
	KICKOUT_LOGINERROR("10002","登陆过程失败T下线"),
	KICKOUT_CREATENEWHEROERROR("10003","创建新角色失败T下线"),
	KICKOUT_NOTCREATETOKEN("10004","登陆账号密码错误"),
	KICKOUT_OTHERLOGIN("10005","其他地方登陆"),
	KICKOUT_ACCOUNTNOTFIND("10006","登陆账号不存在"),
	KICKOUT_ACCOUNTPSWERROR("10007","账号登陆的密码错误"),
	KICKOUT_ACCOUNTAUTHORIZATIONFAIL("10008","第3方登陆验证失败"),
	KICKOUT_TOKENEXPIRE("10009","登陆的token已经过期"),
	HTTP_SERVERNOTSTART("10010","http请求服务器未开启"),

	HTTP_PACKRUNERROR("10011","http请求执行崩溃"),
	HTTP_PACKNOTACTION("10012","http请求没有回复动作,或者条件没有通过不执行"),
	HTTP_NOTFINDPACK("10013","http请求没有这个封包请求"),
	HTTP_REQUESTACCOUNTSERVERFAIL("10014","请求账号服务器失败"),
	HTTP_REQUESTORDERSERVERFAIL("10015","请求订单服务器失败"),
	KICKOUT_SERVERCLOSE("10016","服务器还未开启成功"),
	KICKOUT_ACCOUNTLOGINERROR("10017","账号登录过程失败"),
	KICKOUT_ACCOUNTTOKENERROR("10018","自定义账号登录token验证账号ID失败"),
	KICKOUT_NOTFREEPORTID("10019","没有多余端口登录"),
	PACKRUN_NOTFINDPACK("10020","客户端请求封包逻辑不存在"),
	KICKOUT_MOBILEAUTHORIZATIONFAIL("10023","手机登陆验证失败"),
	KICKOUT_MOBILEEXISTS("10026","手机已经存在，不是使用"),
	KICKOUT_MOBILENOTEXISTS("10028","手机号码不存在"),

	KICKOUT_CREATE_TOKEN_ERROR("10029","创建Token失败！"),
	KICKOUT_AES_ENCRYPT_TOKEN_ERROR("10030","数据加密出现异常！"),
	KICKOUT_AES_DECRYPT_TOKEN_ERROR("10031","数据解密出现异常！"),

	TOKEN_VALIDATION_FAILED_ERROR("10032","验证Token失败！"),
	TOKEN_ACCOUNT_ID_ERROR("10033","验证token后accountId错误！"),
	TOKEN_ACCOUNT_TYPE_ERROR("10033","验证token后accountType错误！"),


	;

	/** 错误码 */
	private String resultCode;

	/** 错误描述 */
	private String resultMsg;

	CommonEnum(String resultCode, String resultMsg) {
		this.resultCode = resultCode;
		this.resultMsg = resultMsg;
	}

	@Override
	public String getResultCode() {
		return resultCode;
	}

	@Override
	public String getResultMsg() {
		return resultMsg;
	}


	public static void main(String[] args) {
		for (CommonEnum commonEnum : CommonEnum.values()) {
			System.out.println(commonEnum.name().toUpperCase());
		}
	}
}