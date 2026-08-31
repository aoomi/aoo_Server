package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.*;

import java.util.List;

@SuppressWarnings("serial")
public class SXCPDK_RoomRecord<T> extends BaseSendMsg {

	public List<T> records;

	public static <T> SXCPDK_RoomRecord<T> make(List<T> records) {
		SXCPDK_RoomRecord<T> ret = new SXCPDK_RoomRecord<T>();
		ret.records = records;

		return ret;

	}
}
