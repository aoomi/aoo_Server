package jsproto.c2s.cclass.pk.base;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

public class CPK_OpCard extends BaseSendMsg {

    public long roomID;
    public int setID;
    public int roundID;
    public int score; //分数
    public int cardID;
    public int opType;
    public List<Integer> cardList = null;//黑风列表

    public static CPK_OpCard make(long roomID, int setID, int roundID, int cardID, int opType) {
        CPK_OpCard ret = new CPK_OpCard();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.roundID = roundID;
        ret.cardID = cardID;
        ret.opType = opType;

        return ret;


    }
}