package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 玩家操作
 * @author huaxing
 *
 */

public class CZYPK_PlayerOp extends BaseSendMsg {
    public long roomID;//房间号
    public int pos;  //位置
    public int opType;//操作类型
    public int anNiuType = -1;//操作类型
    public int addBet;  //押注分数
    public int comporePos;//比较位置
    public ArrayList<Integer> cardList;//牌列表

    public static CZYPK_PlayerOp make(long roomID,int pos, int opType,int anNiuType,int addBet,int comporePos,ArrayList<Integer> cardList) {
    	CZYPK_PlayerOp ret = new CZYPK_PlayerOp();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.anNiuType = anNiuType;
        ret.addBet = addBet;
        ret.comporePos = comporePos;
        ret.cardList = cardList;
        return ret;
    }

}
