package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.room.BaseCreateRoom;

/*
 * 创建房间配置
 * */
public class ZYPKRoom_Cfg extends BaseCreateRoom {
	public int chouMa = 0; //筹码
	public ArrayList<Integer> chuPais = new ArrayList<Integer>();//除牌
	public int liuPai = 0;//留牌
	public int liPai = 0;//理牌
	public List<Integer> anNius = new ArrayList<Integer>();//通用按钮
	public int zhuagnJia = 0;//庄家
	public int xuanZhuang = 0;//选庄
	public int kongPai = 0;//控牌
	public int moShi = 0;//模式
	public boolean isKeptOutAfterStartGame = false;
}
