package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 玩家回放记录
 *
 * @author Huaxing
 */
@Data
public class CPlayer_PlayBack extends BaseSendMsg {
    public int playBackCode;
    public boolean chekcPlayBackCode;

    public static CPlayer_PlayBack make(int playBackCode, boolean chekcPlayBackCode) {
        CPlayer_PlayBack ret = new CPlayer_PlayBack();
        ret.playBackCode = playBackCode;
        ret.chekcPlayBackCode = chekcPlayBackCode;
        return ret;
    }


}

