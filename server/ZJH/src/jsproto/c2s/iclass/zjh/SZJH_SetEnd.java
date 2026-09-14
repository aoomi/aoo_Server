package jsproto.c2s.iclass.zjh;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.Victory;


public class SZJH_SetEnd extends BaseSendMsg {
    public long roomID;
    public int setStatus;
    public long startTime;
    public List<Integer> crawTypeList ;//牛牛类型
	public List<Integer> pointList; //得分
	public List<Integer> statusCardList;
	public HashMap<Integer,ArrayList<Victory> > recordList;
	public Victory  refund;//全压退款
	public ArrayList<ArrayList<Integer>> cards = new ArrayList<ArrayList<Integer>>();	//牌

    public static SZJH_SetEnd make(long roomID,int setStatus, long startTime, List<Integer> crawTypeList, List<Integer> pointList,List<Integer> statusCardList,HashMap<Integer,ArrayList<Victory> > recordList, Victory  refund, ArrayList<ArrayList<Integer>> cards) {
    	SZJH_SetEnd ret = new SZJH_SetEnd();
        ret.roomID = roomID;
        ret.setStatus = setStatus;
        ret.startTime = startTime;
        ret.crawTypeList = crawTypeList;
        ret.pointList = pointList;
        ret.statusCardList = statusCardList;
        ret.recordList = recordList;
        ret.refund = refund;
        ret.cards = cards;
        return ret;
    }
}
