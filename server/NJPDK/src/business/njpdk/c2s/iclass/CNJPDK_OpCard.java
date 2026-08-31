package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;

/**
 * 接收客户端数据
 *
 * @author zaf
 */

public class CNJPDK_OpCard extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int opCardType;  //PDK_CARD_TYPE 操作类型及牌的类型
    public ArrayList<Integer> cardList;
    public int daiNum;//带几张牌
    public boolean isFlash = false;
    public boolean guanpai;

    public static CNJPDK_OpCard make(long roomID, int pos, int opCardType, ArrayList<Integer> cardList, int daiNum, boolean isFlash) {
        CNJPDK_OpCard ret = new CNJPDK_OpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        ret.daiNum = daiNum;
        ret.isFlash = isFlash;
        return ret;
    }

}
