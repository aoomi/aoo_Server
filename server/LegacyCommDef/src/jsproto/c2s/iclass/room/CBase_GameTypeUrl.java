package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取游戏连接地址
 *
 * @author xushaojun
 */
@Data
public class CBase_GameTypeUrl extends BaseSendMsg {
    /**
     * 游戏ID
     */
    private int gametype;


}