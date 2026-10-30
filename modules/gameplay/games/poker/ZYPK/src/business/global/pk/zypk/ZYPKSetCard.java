package business.global.pk.zypk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.security.SecureRandom;

import com.ddm.server.common.utils.CommMath;
import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;

import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.BasePocker.PockerListType;

/**
 * 自由扑克，设置牌
 * @author Huaxing
 *
 */
public class ZYPKSetCard {
	public final int SaiziCnt = 2; // 筛子数量
	public List<Integer> Saizi = new ArrayList<>(); // 筛子点数
	
	public ArrayList<Integer> leftCards = new ArrayList<Integer>(); // 扑克牌编号
	private static final SecureRandom SEEDS = new SecureRandom();
	private final GameRandomSource random;
	private ZYPKRoom<?> room;
	public ZYPKSetCard(ZYPKRoom<?> room){
		this(room, new SeededGameRandomSource(SEEDS.nextLong()));
	}
	public ZYPKSetCard(ZYPKRoom<?> room, GameRandomSource random){
		this.room = java.util.Objects.requireNonNull(room, "room");
		this.random = java.util.Objects.requireNonNull(random, "random");
	}
	
	/**
	 * 初始化扑克牌
	 */
	public void init() {
		// 获取扑克编号
		this.leftCards = BasePockerLogic.getRandomPockerList(1, 1, PockerListType.POCKERLISTTYPE_TWOEND);
//		//判断是否符合发牌条件
//		if(!this.isSetCard()) {
//			return;
//		}
		//除去不要的扑克
		for (Integer chuPai : room.getChuPais()) {
			BasePockerLogic.deleteSameCard(this.leftCards, chuPai, false);
		}		
	}
	
	
	
	
	/**
	 * 是否符合
	 * @return
	 */
	public boolean isSetCard() {
		//总扑克数量
		int leftCardCount = this.leftCards.size();
		//除去扑克数量
		int chuPaiCount = this.room.getChuPais().size();
		//扑克留牌数量
		int liuPaiCount = this.room.getLiuPaiNum();
		if (leftCardCount <= chuPaiCount + liuPaiCount) {
			//推送
			return false ;
		}
		return true;
	}
	
	
	public void addLeftCard(Integer cardByte) {
		if (null == cardByte)
			return;
		this.leftCards.add(cardByte);
	}
	
	/**
	 * 洗牌
	 */
	public void  onXiPai() {
		this.random.shuffle(this.leftCards);
	}
	
	/**
	 * 没有足够的牌
	 * @param privateCard
	 * @param publicCard
	 * @param playerNum
	 * @return
	 */
	public boolean isNotEnough (int privateCard,int publicCard,int playerNum) {
		int sizeCard = (privateCard * playerNum) + publicCard;
		int sizeLeftCard = this.leftCards.size();
		if (sizeLeftCard - sizeCard >= 0) {
			return true;
		}
		
		return true;
	}
	
	/**
	 * 发牌
	 * @param cnt
	 * @return
	 */
	public ArrayList<Integer> popList(int cnt){
		ArrayList<Integer> ret = new ArrayList<Integer>();
		if(this.leftCards.size() <= 0) return ret;
		for (int i = 0; i < cnt; i++) {
			if(this.leftCards.size() <= 0) return ret;
			Integer byte1 = this.leftCards.remove(random.nextInt(this.leftCards.size()));
			ret.add(byte1);
		}
		return ret;
	}
	
	/**
	 * 获取一张牌
	 * @return
	 */
	public Integer pop() {
		if (this.leftCards.size() <= room.getLiuPaiNum()) {
			return null;
		}
		Integer pkByte = this.leftCards.remove(random.nextInt(this.leftCards.size()));
		return pkByte;
	}
	
	
	/**
	 * 随机筛子
	 * @return
	 */
	public int randomSaizi(int playerNum) {
		int totalPoint = 0;
		for (int i = 0; i < this.SaiziCnt; i++) {
			int tmp = CommMath.randomInt(1, 6);
			this.Saizi.add(tmp);
			totalPoint += tmp;
		}
		return (totalPoint - 1) % playerNum;
	}
		
	/**
	 * 通过抢庄列表
	 * 随机获取庄家
	 * @param dPosList
	 * @param playerNum
	 * @return
	 */
	public int randomDPos(List<Integer> dPosList,int playerNum) {
		if (dPosList.size() <= 0) {
			return randomSaizi(playerNum);
		} else if (dPosList.size() == 1) {
			return dPosList.get(0);
		}
		return dPosList.remove(this.random.nextInt(dPosList.size()));
	}
	
	/**
	 * 获取剩余的扑克数量
	 * @return
	 */
	public int getPKCardSize() {
		return this.leftCards.size();
	}
	
}

