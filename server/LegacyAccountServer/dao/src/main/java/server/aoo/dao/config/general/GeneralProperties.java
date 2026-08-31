package server.aoo.dao.config.general;

import jsproto.c2s.cclass.general.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class GeneralProperties {
    /**
     * 通用配置
     */
    private GeneralInfo general;
    /**
     * 令牌信息
     */
    private TokenInfo token;

    /**
     * 微信
     */
    private WeChat wechat;

    /**
     * 手机
     */
    private Mobile mobile;

    /**
     * 日志等级
     */
    private LogLevelD logLevelD;
}
