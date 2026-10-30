package jsproto.c2s.iclass.mj;

import cenum.room.SetState;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;

import java.util.List;

/**
 * 接收客户端数据
 * 状态改变
 *
 * @author zaf
 */

@SuppressWarnings("serial")
public class _ChangeStatus extends BaseSendMsg {

    public long roomID;
    public int setID;//局数
    public int dPos;//局数
    public SetState state;  //位置
    public String waitingExType;  //票分类型
    public List<MJTemplateWaitingExInfo> biaoShiList;

    public static _ChangeStatus make(long roomID, int setID, SetState state, int dPos, String waitingExType, List<MJTemplateWaitingExInfo> biaoShiList, String gameNameStr) {
        _ChangeStatus ret = new _ChangeStatus();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.state = state;
        ret.dPos = dPos;
        ret.waitingExType = waitingExType;
        ret.biaoShiList = biaoShiList;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}																		
