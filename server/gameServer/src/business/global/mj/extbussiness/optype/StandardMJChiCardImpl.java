package business.global.mj.extbussiness.optype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJSetPos;
import business.global.mj.manage.OpCard;
import business.global.mj.set.LastOpTypeItem;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模板麻将吃
 */
public class StandardMJChiCardImpl implements OpCard {

	@Override
	public boolean checkOpCard(AbsMJSetPos mSetPos, int cardId) {
		if (((StandardMJSetPos)mSetPos).isTing()) {
			return false;
		}
		int cardType = cardId / 100;
		MJCardInit mjCardInit = mSetPos.mjCardInit(false);
		if (null == mjCardInit)
			return false;
		List<List<Integer>> chiList = new ArrayList<List<Integer>>();

		Map<Boolean, List<Integer>> partitioned = mjCardInit.getAllCardInts()
				.stream().collect(Collectors.partitioningBy(e -> e >= 40));
		if (null == partitioned) {
			return false;
		}
		List<Integer> allCardList = new ArrayList<>();
		if (cardType < 40) {
			allCardList.addAll(partitioned.get(false));
			CommMath.getSort(allCardList, true);
			chiList = chiCard(allCardList, cardType, 0, chiList);
		} else if(((StandardMJRoom)mSetPos.getRoom()).isChiDaZhi()){
			allCardList.addAll(partitioned.get(true));
			CommMath.getSort(allCardList, true);
			chiList = chiDaPaiCard(allCardList,cardType,chiList);
		}

		List<List<Integer>> newChiList = new ArrayList<List<Integer>>();
		for (int i = 0; i < chiList.size(); i++) {
			List<Integer> chis = chiList.get(i);
			chis = chiListCarId(chis, mSetPos.getPrivateCard());
			if (chis.size() < 3)
				chis.add(cardId);
			chiSort(chis);
			if(mSetPos.getSet().isCanChiPeng(chis,mSetPos,chis.get(2)/100+1,cardId/100)){
				newChiList.add(chis);
			}
		}

		if (newChiList.size() <= 0) {
			return false;
		}
		mSetPos.getPosOpNotice().setChiList(newChiList);
		return true;
	}

	/**
	 * 获取吃列表ID
	 *
	 * @param chis
	 * @param privateCards
	 * @return
	 */
	private List<Integer> chiListCarId(List<Integer> chis,
									   List<MJCard> privateCards) {
		List<Integer> chiCardIds = new ArrayList<Integer>();
		List<Integer> chiCardCount = new ArrayList<Integer>();

		for (int i = 0, size = privateCards.size(); i < size; i++) {
			if (chis.contains(privateCards.get(i).type)) {
				int cardType = privateCards.get(i).type;
				if (!chiCardCount.contains(cardType)) {
					chiCardCount.add(cardType);
					chiCardIds.add(privateCards.get(i).cardID);
				}

			}
			if (chiCardCount.size() >= 3)
				break;
		}
		return chiCardIds;

	}

	/**
	 * 排序
	 *
	 * @param chis
	 */
	private void chiSort(List<Integer> chis) {
		Collections.sort(chis, (o1, o2) -> {
			int oType1 = o1 / 100;
			int oType2 = o2 / 100;
			return oType1 - oType2;
		});
	}


	/**
	 * 吃操作
	 */
	@Override
	public boolean doOpCard(AbsMJSetPos cSetPos, int cardID) {
		boolean ret = false;
		StandardMJSetPos mSetPos = (StandardMJSetPos) cSetPos;
		int lastOutCard = mSetPos.getSet().getLastOpInfo().getLastOutCard();
		int fromPos = mSetPos.getMJSetCard().getCardByID(lastOutCard).ownnerPos;
		List<Integer> publicCard = new ArrayList<>();
		List<Integer> chiTmps = new ArrayList<Integer>();
		publicCard.add(OpType.Chi.value());
		publicCard.add(fromPos);
		publicCard.add(lastOutCard);
		List<Integer> chiCardList = mSetPos.getChiCardList();
		if (chiCardList != null && chiCardList.size() >= 3) {
			chiTmps.addAll(chiCardList);
			int lastOutCardType = lastOutCard / 100;
			for (int i = 0; i < chiTmps.size(); i++) {
				if (lastOutCardType == chiTmps.get(i) / 100)
					chiTmps.set(i, lastOutCard);
			}
		} else {
			for (int i = 0; i < mSetPos.getPosOpNotice().getChiList().size(); i++) {
				List<Integer> chis = mSetPos.getPosOpNotice().getChiList().get(i);
				for (int j = 0; j < chis.size(); j++) {
					if (chis.get(0) == cardID) {
						chiTmps = chis;
						break;
					}
				}
			}

			int lastOutCardType = lastOutCard / 100;
			for (int i = 0; i < chiTmps.size(); i++) {
				if (lastOutCardType == chiTmps.get(i) / 100)
					chiTmps.set(i, lastOutCard);
			}
		}
		// 搜集牌
		List<MJCard> tmp = new ArrayList<>();
		for (int i = 0; i < mSetPos.getPrivateCard().size(); i++) {
			if (chiTmps.contains(mSetPos.getPrivateCard().get(i).cardID)) {
				if (!tmp.contains(mSetPos.getPrivateCard().get(i)))
					tmp.add(mSetPos.getPrivateCard().get(i));
				if (tmp.size() >= 2) {
					ret = true;
					break;
				}
			}
		}
		if (ret) {
			publicCard.add(chiTmps.get(0));
			publicCard.add(chiTmps.get(1));
			publicCard.add(chiTmps.get(2));
			mSetPos.addPublicCard(publicCard);
			mSetPos.removeAllPrivateCard(tmp);
			mSetPos.getSet().getSetPosMgr().clearChiList();
			mSetPos.getSet().getLastOpInfo().addLastOpItem(OpType.Chi,new LastOpTypeItem(mSetPos.getPosID(),lastOutCard));
			mSetPos.privateMoveHandCard();
		}
		return ret;
	}

	/**
	 * 获取吃牌
	 * 万条筒
	 *
	 * @param privateCards 私有牌
	 * @param cardType     牌类型
	 * @param idx          位置
	 * @param chiList      吃列表
	 * @return
	 */
	public List<List<Integer>> chiCard(List<Integer> privateCards,
									   int cardType, int idx, List<List<Integer>> chiList) {
		List<Integer> cardInts = new ArrayList<Integer>();
		// 如果 下标 和 手牌长度一致
		if (idx == privateCards.size())
			return chiList;
		// 从指定的下标开始，遍历出所有手牌
		for (int i = idx, size = privateCards.size(); i < size; i++) {
			// 如果 手牌中的类型 == 牌的类型
			if (privateCards.get(i) == cardType || privateCards.get(i) > 39)
				continue;
			// 如果 手牌中类型 不出现重复 并且 记录的牌数 < 2
			if (!cardInts.contains(privateCards.get(i)) && cardInts.size() < 2) {
				// 添加不重复的牌
				cardInts.add(privateCards.get(i));
				// 如果 记录牌数 == 2 结束循环
			} else if (cardInts.size() == 2)
				break;
		}
		idx++;
		// 如果 记录牌数 == 2
		if (cardInts.size() == 2) {
			// 添加牌
			cardInts.add(cardType);
			// 判断是否顺子
			if (CommMath.isContinuous(cardInts)) {
				// 如果是否有重复的顺子
				if (!chiList.contains(cardInts))
					chiList.add(cardInts);
				return chiCard(privateCards, cardType, idx, chiList);
			}
		}
		return chiCard(privateCards, cardType, idx, chiList);
	}


//	/**
//	 * 获取吃牌
//	 * 吃风牌
//	 * @param privateCards
//	 *            私有牌
//	 * @param cardType
//	 *            牌类型
//	 * @param idx
//	 *            位置
//	 * @param chiList
//	 *            吃列表
//	 * @return
//	 */
//	public List<List<Integer>> chiFengCard(List<Integer> privateCards,
//			int cardType, int idx, List<List<Integer>> chiList) {
//		List<Integer> cardInts = new ArrayList<Integer>();
//		// 如果 下标 和 手牌长度一致
//		if (idx == privateCards.size())
//			return chiList;
//		// 从指定的下标开始，遍历出所有手牌
//		for (int i = idx, size = privateCards.size(); i < size; i++) {
//			// 如果 手牌中的类型 == 牌的类型
//			if (privateCards.get(i) == cardType || privateCards.get(i) >= 45)
//				continue;
//			// 如果 手牌中类型 不出现重复 并且 记录的牌数 < 2
//			if (!cardInts.contains(privateCards.get(i)) && cardInts.size() < 2) {
//				// 添加不重复的牌
//				cardInts.add(privateCards.get(i));
//				// 如果 记录牌数 == 2 结束循环
//			} else if (cardInts.size() == 2)
//				break;
//		}
//		idx++;
//		// 如果 记录牌数 == 2
//		if (cardInts.size() == 2) {
//			// 添加牌
//			cardInts.add(cardType);
//			// 如果是否有重复的顺子
//			if (!chiList.contains(cardInts))
//				chiList.add(cardInts);
//			return chiFengCard(privateCards, cardType, idx, chiList);
//		}
//		return chiFengCard(privateCards, cardType, idx, chiList);
//	}


	/**
	 * 吃箭牌
	 *
	 * @param privateCards
	 * @param cardType
	 */
	public List<List<Integer>> chiDaPaiCard(List<Integer> privateCards, int cardType, List<List<Integer>> chiLists) {
		Map<Boolean, List<Integer>> partitioned = privateCards
				.stream().collect(Collectors.partitioningBy(e -> e >= 45));
		if (null == partitioned)
			return chiLists;
		List<Integer> cardList = new ArrayList<>();
		List<Integer> chiList = new ArrayList<>();
		if (cardType >= 45) {
			//箭牌列表
			cardList = partitioned.get(true);
			for (Integer card : cardList) {
				//如果遍历的牌和要吃的牌一样，则跳过
				if (card == cardType)
					continue;
				//检查遍历的牌是否在吃列表
				if (!chiList.contains(card)) {
					chiList.add(card);
					if (chiList.size() == 2)
						break;
				}
			}
			if (chiList.size() == 2) {
				chiList.add(cardType);
				chiLists.add(chiList);
			}

			return chiLists;
		} else {
			//风牌列表
			cardList = partitioned.get(false);
			chiLists = chiFengCard(new ArrayList<Integer>(cardList), cardType, 0, chiLists);
			return chiLists;
		}

	}


	/**
	 * 获取吃牌
	 * 吃风牌
	 *
	 * @param privateCards 私有牌
	 * @param cardType     牌类型
	 * @param idx          位置
	 * @param chiList      吃列表
	 * @return
	 */
	public List<List<Integer>> chiFengCard(List<Integer> privateCards,
										   int cardType, int idx, List<List<Integer>> chiList) {
		List<Integer> cardInts = null;
		if (idx == privateCards.size())
			return chiList;
		int curCardType = privateCards.get(idx);
		for (int i = idx, size = privateCards.size(); i < size; i++) {
			//检查当前 下标的牌 == 上家打出的牌
			if (curCardType == cardType) {
				// 证明牌重复
				break;
			}
			// 如果 遍历出的牌和打出的牌一致，跳过本次循环。
			if (privateCards.get(i) == cardType || privateCards.get(i) >= 45 || privateCards.get(i) == curCardType)
				continue;
			cardInts = new ArrayList<Integer>();
			cardInts.add(cardType);
			cardInts.add(curCardType);
			cardInts.add(privateCards.get(i));
			if (!chiList.contains(cardInts))
				chiList.add(cardInts);
		}
		idx++;
		return chiFengCard(privateCards, cardType, idx, chiList);
	}

	
	
}
