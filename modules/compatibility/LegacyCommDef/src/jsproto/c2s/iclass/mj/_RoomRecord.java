package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

@SuppressWarnings("serial")	
public class _RoomRecord<T> extends BaseSendMsg {
	
	public List<T> records;	
	
	public static <T> _RoomRecord<T> make(List<T> records, String gameNameStr) {
		_RoomRecord<T> ret = new _RoomRecord<T>();	
		ret.records = records;	
		ret.setGameNameStr(gameNameStr);
		return ret;	
	
	}	
}	
