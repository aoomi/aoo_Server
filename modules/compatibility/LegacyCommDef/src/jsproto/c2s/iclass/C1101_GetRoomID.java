package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取进入房间标识
 */
@Data
public class C1101_GetRoomID extends BaseSendMsg {
    /**
     * 标识
     */
    private String sign;
}
