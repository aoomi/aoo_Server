package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 房间
 *
 * @author Administrator
 */
@Data
public class CBase_GetRoomCfg extends BaseSendMsg {
    /**
     * 房间key
     */
    private String roomKey;


}