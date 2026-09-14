package jsproto.c2s.iclass.zypk;
import jsproto.c2s.cclass.*;
import jsproto.c2s.cclass.zypk.ZYPK_define.Op_Player;
import jsproto.c2s.cclass.zypk.ZYPK_define.ZYPK_AnNiu;


public class SZYPK_PosOpCard<T> extends BaseSendMsg {
    
    public long roomID;
    public int pos;
    public Op_Player opType;
    public T set_Pos = null;
    
    public ZYPK_AnNiu onAnNiu = ZYPK_AnNiu.Not;
    public int number = -1;
    public int comporePos = -1;//比较位置
    
    public static <T>SZYPK_PosOpCard make(long roomID, int pos, T set_Pos, Op_Player opType,ZYPK_AnNiu onAnNiu,int comporePos) {
    	SZYPK_PosOpCard ret = new SZYPK_PosOpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.set_Pos = set_Pos;
        ret.opType = opType;
        ret.onAnNiu = onAnNiu;
        ret.comporePos = comporePos;
        return ret;
    }
    
    public static SZYPK_PosOpCard make(long roomID, int pos, Op_Player opType,ZYPK_AnNiu onAnNiu,int number) {
    	SZYPK_PosOpCard ret = new SZYPK_PosOpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.onAnNiu = onAnNiu;
        ret.number = number;
        return ret;
    }

    public static <T>SZYPK_PosOpCard make(long roomID, int pos, Op_Player opType,ZYPK_AnNiu onAnNiu,int number,int comporePos,T set_Pos) {
    	SZYPK_PosOpCard ret = new SZYPK_PosOpCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.onAnNiu = onAnNiu;
        ret.number = number;
        ret.comporePos = comporePos;
        ret.set_Pos = set_Pos;
        return ret;
    }

}
