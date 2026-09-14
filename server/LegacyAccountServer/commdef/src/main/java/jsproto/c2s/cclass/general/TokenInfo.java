package jsproto.c2s.cclass.general;

import lombok.Data;

/**
 * 令牌信息
 */
@Data
public class TokenInfo {
    /**
     * 创建账号登陆token的加密key
     */
    private String TokenSecret;
    /**
     * token过期时间一分钟
     */
    private String TokenExpireTick;
}
