package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 每局开始
 * @author leo_wi
 * @param <T>
 */
@SuppressWarnings("serial")	
public class _SetStart<T> extends BaseSendMsg {
    	
    public long roomID;	
    public T setInfo;	
	
	
    public static <T>_SetStart<T> make(long roomID, T setInfo,String gameNameStr) {
        _SetStart<T> ret = new _SetStart<T>();
        ret.roomID = roomID;	
        ret.setInfo = setInfo;	
        //打印数组看看	
        ret.setGameNameStr(gameNameStr);
        return ret;	
    }	
}	
