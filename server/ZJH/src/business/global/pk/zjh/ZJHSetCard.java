package business.global.pk.zjh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;

import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.BasePocker.PockerListType;




/**
 * 炸金花，设置牌
 * @author zaf
 *
 */
public class ZJHSetCard {

	public List<Integer> leftCards = new LinkedList<>(); // 扑克牌编号
	private final GameRandomSource random;
	public ZJHSetCard(Object ignoredSet){
		this(ignoredSet, SeededGameRandomSource.create());
	}

	ZJHSetCard(Object ignoredSet, GameRandomSource random){
		this.random = java.util.Objects.requireNonNull(random, "random");
		this.randomCard();
	}
	

	/**
	 * 洗牌
	 */
	public void randomCard(){
		this.leftCards = BasePockerLogic.getRandomPockerList(1, 0, PockerListType.POCKERLISTTYPE_AEND);
	}
	
	/*
	 * 洗牌
	 * **/
	public void  onXiPai() {
		random.shuffle(this.leftCards);
	}

	/**
	 * 发牌
	 * @param cnt
	 * @return
	 */
	public List<Integer> popList(int cnt){
		List<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < cnt; i++) {
			Integer byte1 = this.leftCards.remove(random.nextInt(this.leftCards.size()));
			ret.add(byte1);
		}
		return ret;
	}
}

