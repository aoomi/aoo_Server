package business.secret;

import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.common.utils.AesEncryptUtils;
import com.ddm.server.common.utils.RSACoder;
import com.ddm.server.enums.CommonEnum;
import com.ddm.server.exception.BizException;
import com.ddm.server.redis.jedis.RedisMap;
import com.ddm.server.redis.jedis.RedisSource;
import com.google.common.collect.Maps;
import lombok.Data;
import org.springframework.stereotype.Component;
import server.aoo.dao.utils.ConfigUtils;

import java.security.Key;
import java.util.Map;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家管理
 *
 * @author Hxing
 */
@Data
@Component
public class SecretManager {
    /**
     * 密钥管理
     */
    private final RedisMap redisSecretMap = RedisSource.getMap(RedisBydrKeyEnum.SECRET.getKey());

    /**
     * 密钥管理
     * 存储内存
     */
    private final Map<String,String> secretMap = Maps.newConcurrentMap();


    /**
     * 初始化秘钥管理器
     */
    public void init() {
        if (this.redisSecretMap.isEmpty()) {
            // 刷新秘钥对
            this.refreshSecret();
        } else {
            this.secretMap.putAll(this.redisSecretMap.toMap());
        }
        validateTokenSecret(this.secretMap.get(AesEncryptUtils.AES_ENCRYPT));
    }

    /**
     * 刷新秘钥对
     */
    public void refreshSecret() {
        try {
            String tokenSecret = ConfigUtils.getTokenSecret();
            validateTokenSecret(tokenSecret);
            Map<String, Key> initKey =  RSACoder.initKey();
            this.secretMap.put(RSACoder.PUBLIC_KEY, RSACoder.getPublicKey(initKey));
            this.secretMap.put(RSACoder.PRIVATE_KEY, RSACoder.getPrivateKey(initKey));
            this.secretMap.put(AesEncryptUtils.AES_ENCRYPT, tokenSecret);
            this.redisSecretMap.putAll(this.secretMap);
        } catch (Exception e) {
            throw BizException.Of(CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultCode(),CommonEnum.KICKOUT_ACCOUNTNOTFIND.getResultMsg(),e);
        }
    }

    private void validateTokenSecret(String tokenSecret) {
        int length = tokenSecret == null ? 0 : tokenSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("ACCOUNT_TOKEN_SECRET must contain 16, 24, or 32 UTF-8 bytes");
        }
    }

    /**
     * 创建账号登陆token的加密key
     * @return
     */
    public final String getTokenSecret() {
        return this.secretMap.get(AesEncryptUtils.AES_ENCRYPT);
    }

    /**
     * 获取公钥
     * @return
     */
    public final String getPublicKey() {
        return this.secretMap.get(RSACoder.PUBLIC_KEY);
    }

    /**
     * 获取私钥
     * @return
     */
    public final String getPrivateKey() {
        return this.secretMap.get(RSACoder.PRIVATE_KEY);
    }


}
