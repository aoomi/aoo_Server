package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CPlayer_SignInterface extends BaseSendMsg {
    /**
     * 标识值
     */
    private int sign;

}
