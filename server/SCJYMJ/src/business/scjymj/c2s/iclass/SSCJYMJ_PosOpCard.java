package business.scjymj.c2s.iclass;

import cenum.mj.OpType;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;


public class SSCJYMJ_PosOpCard<T> extends BaseSendMsg {

    public long roomID;
    public int pos;
    public T set_Pos;
    public OpType opType;
    public int opCard;
    public boolean isFlash;
    private List<String> dingQueList = new ArrayList<>();

    public static <T> SSCJYMJ_PosOpCard make(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash) {
        SSCJYMJ_PosOpCard ret = new SSCJYMJ_PosOpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.set_Pos = set_Pos;
        ret.opType = opType;
        ret.opCard = opCard;
        ret.isFlash = isFlash;
        return ret;
    }

    public void setDingQueList(List<String> dingQueList) {
        this.dingQueList = dingQueList;
    }

    public void setSet_Pos(T set_Pos) {
        this.set_Pos = set_Pos;
    }
}
