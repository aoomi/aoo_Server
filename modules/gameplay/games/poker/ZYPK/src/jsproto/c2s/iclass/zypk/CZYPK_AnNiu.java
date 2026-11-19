package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 通用按钮设置
 * @author huaxing
 *
 */

public class CZYPK_AnNiu extends BaseSendMsg {

    public long roomID;//房间号
    public int pos;  //位置
    public int opType;//操作类型
    public int anNiuType;//操作类型
    public int addBet;  //押注分数
    public int comporePos;//比较位置
    public ArrayList<Integer> cardList;//牌列表

    public static CZYPK_AnNiu make(long roomID,int pos, int opType,int anNiuType,int addBet,int comporePos,ArrayList<Integer> cardList) {
    	CZYPK_AnNiu ret = new CZYPK_AnNiu();
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
