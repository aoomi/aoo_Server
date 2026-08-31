package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

public class CMJ_OpCard extends BaseSendMsg {

    public long roomID;
    public int setID;
    public int roundID;
    public int cardID;
    public int opType;
    public List<Integer> cardList = null;//黑风列表
    public static List<Integer> card = null;//听游金的牌ID List

    public static CMJ_OpCard make(long roomID, int setID, int roundID, int cardID, int opType) {
        CMJ_OpCard ret = new CMJ_OpCard();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.roundID = roundID;
        ret.cardID = cardID;
        ret.opType = opType;

        return ret;


    }
}