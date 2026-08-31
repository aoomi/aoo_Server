package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 控牌动作
 * @author huaxing
 *
 */

public class CZYPK_KongPai extends BaseSendMsg {

    public long roomID;//房间号
    public int pos;  //位置
    public int opType;//操作类型
    public int privateCount = 0;//私有牌
    public int publicCount = 0;//公共牌
    public static CZYPK_KongPai make(long roomID,int pos, int opType,int privateCount,int publicCount) {
    	CZYPK_KongPai ret = new CZYPK_KongPai();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.privateCount = privateCount;
        ret.publicCount = publicCount;
        return ret;
    }
}
