package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;

import java.util.List;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class SMJ_PiaoFen extends BaseSendMsg {

    public long roomID;
    public int pos;
    public List<MJTemplateWaitingExInfo> biaoShiList;

    public static SMJ_PiaoFen make(long roomID, int pos, List<MJTemplateWaitingExInfo> biaoShiList) {
        SMJ_PiaoFen ret = new SMJ_PiaoFen();
        ret.roomID = roomID;
        ret.biaoShiList = biaoShiList;
        ret.pos = pos;
        return ret;
    }
}
