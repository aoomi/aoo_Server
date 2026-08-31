package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.Map;

/**
 * 接收客户端数据
 * 记牌器
 *
 * @author zaf
 */

public class SNJPDK_CardNumber extends BaseSendMsg {

    public long roomID;
    public Map<Integer, Integer> cardNumMap;

    public static SNJPDK_CardNumber make(long roomID, Map<Integer, Integer> cardNumMap) {
        SNJPDK_CardNumber ret = new SNJPDK_CardNumber();
        ret.cardNumMap = cardNumMap;
        ret.roomID = roomID;
        return ret;
    }
}
