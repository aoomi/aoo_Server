package jsproto.c2s.iclass.mj;

import cenum.mj.OpType;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class SMJ_OpPass extends BaseSendMsg {

    public long roomID;
    public OpType opType;
    public int pos;
    public long doTime;

    public void make(long roomID, int pos, OpType opType, long doTime) {
        this.roomID = roomID;
        this.pos = pos;
        this.doTime = doTime;
        this.opType = opType;
    }

}
