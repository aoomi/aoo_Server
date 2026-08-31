package jsproto.c2s.cclass.user;

import com.google.gson.JsonObject;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;

@Data
public class UserValidityInfo implements Serializable {
    /**
     * 令牌信息
     */
    private JsonObject tokenInfo;
    /**
     * 玩家信息
     */
    private JsonObject playerInfo;

    public UserValidityInfo(JsonObject tokenInfo, JsonObject playerInfo) {
        this.tokenInfo = tokenInfo;
        this.playerInfo = playerInfo;
    }

    public static final UserValidityInfo Of(JsonObject tokenInfo, JsonObject playerInfo) {
        return new UserValidityInfo(tokenInfo,playerInfo);
    }

    public boolean existError() {
        return ObjectUtils.allNotNull(this.tokenInfo,this.playerInfo);
    }

}
