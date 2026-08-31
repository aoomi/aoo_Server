package jsproto.c2s.iclass.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.room.BaseCreateRoom;

/**
 * 自由扑克
 * 接收客户端数据
 * 创建房间
 * @author huaxing
 *
 */

public class CZYPK_CreateRoom extends BaseCreateRoom {
	public int chouMa = 0; //筹码
	public ArrayList<Integer> chuPais = new ArrayList<Integer>();//除牌
	public int liuPai = 0;//留牌
	public int liPai = 0;//理牌
	public List<Integer> anNius = new ArrayList<Integer>();//通用按钮
	public int xuanZhuang = 0;//选庄时间
	public int kongPai = 0;//控牌设置
	public int moShi = 0;//出牌模式
	public int zhuangJia = 0;//有无庄家
    public static CZYPK_CreateRoom make(int setCount,int paymentRoomCardType, int playerNum, ArrayList<Integer> kexuanwanfa, boolean isContinue,long clubID, long gameIndex,
    		int chouMa,ArrayList<Integer> chuPais,int liuPai,int liPai,List<Integer> anNius,int zhuangJia,int xuanZhuang,int kongPai,int moShi,int createType) {
    	CZYPK_CreateRoom ret = new CZYPK_CreateRoom();
		ret.setSetCount(setCount);
		ret.setPlayerNum(playerNum);
		ret.setPaymentRoomCardType(paymentRoomCardType);
		ret.setKexuanwanfa(kexuanwanfa);
		ret.setClubId(clubID);
		ret.setGameIndex(gameIndex);
		ret.setCreateType(createType);
        
        ret.chouMa = chouMa;
        ret.chuPais = chuPais;
        ret.liuPai = liuPai;
        ret.liPai = liPai;
        ret.anNius = anNius;
        ret.zhuangJia = zhuangJia;
        ret.xuanZhuang = xuanZhuang;
        ret.kongPai = kongPai;
        ret.moShi = moShi;
        return ret;
    }
}
