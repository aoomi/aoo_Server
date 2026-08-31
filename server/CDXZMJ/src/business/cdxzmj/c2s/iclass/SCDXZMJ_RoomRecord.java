package business.cdxzmj.c2s.iclass;				
				
import jsproto.c2s.cclass.*;				
				
import java.util.List;				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_RoomRecord<T> extends BaseSendMsg {				
				
	public List<T> records;				
				
	public static <T> SCDXZMJ_RoomRecord<T> make(List<T> records) {				
		SCDXZMJ_RoomRecord<T> ret = new SCDXZMJ_RoomRecord<T>();				
		ret.records = records;				
				
		return ret;				
				
	}				
}				
