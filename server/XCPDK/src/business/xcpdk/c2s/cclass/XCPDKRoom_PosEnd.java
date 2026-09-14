package business.xcpdk.c2s.cclass;

import java.util.ArrayList;

/**
 * 跑得快 配置
 * @author zaf
 *
 */

// 位置结束的信息
public class XCPDKRoom_PosEnd{
	public int pos = 0; //位置
	public long pid = 0;//玩家pid
	public int point = 0; // 本局积分变更
	public int 			doubleNum = 0;				//加倍
	public long baseMark = 0;// 底分
	public int 			robClose;					//春天-1:反春天 0:没有春天或者饭反春天 1:春天
	public ArrayList<Integer> surplusCardList = new ArrayList<Integer>();		//剩余牌数
	/**
	 * 竞技点
	 */
	public Double sportsPoint;
	public boolean xiaoGuan = false;
	public boolean beShutDow = false;
}
