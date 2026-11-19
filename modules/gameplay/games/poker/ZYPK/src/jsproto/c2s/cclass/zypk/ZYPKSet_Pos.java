package jsproto.c2s.cclass.zypk;

import java.util.ArrayList;
import java.util.List;

/**
 * 自由扑克 配置
 * @author Clark
 *
 */
// 一局中各位置的信息
public class ZYPKSet_Pos{
	public int posID = -1;
	public ArrayList<Integer> privateCards = new ArrayList<>(); // 私有牌
	public ArrayList<Integer> outCards = new ArrayList<Integer>(); // 打出牌

}	
