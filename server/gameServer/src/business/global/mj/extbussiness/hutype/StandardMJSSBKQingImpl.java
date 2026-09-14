package business.global.mj.extbussiness.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.hu.abs.AbsSSBK;
import cenum.mj.MJCardCfg;
import cenum.mj.MJSpecialEnum;
import com.ddm.server.common.utils.CommMath;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 麻将七星十三浪
 */
public class StandardMJSSBKQingImpl extends AbsSSBK {
	@Override
	public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
		if (mSetPos.sizePublicCardList() > 0) {
			return false;
		}
		return checkQSSBK(mCardInit.getAllCardInts(),mCardInit.sizeJin());
	}

	/**
	 * 检查用户是否胡
	 *
	 * @param allCards
	 * @return
	 */
	protected boolean checkQSSBK(List<Integer> allCards,int sieJin) {
		int size = (int) allCards.stream().distinct().count();
		if (size != allCards.size()) {
			return false;
		}
		List<Integer> list = allCards.stream().filter(k -> k < MJSpecialEnum.FENG.value()).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(list)) {
			return false;
		}
		List<Integer> fengList = allCards.stream().filter(k -> k > MJSpecialEnum.FENG.value()).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		if ((fengList.size()+sieJin)<7) {
			return false;
		}
		return checkCard(list);
	}

	@Override
	public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
		if (mSetPos.sizePublicCardList() > 0) {
            return null;
        }
		StandardMJPointItem item=null;
		if (checkQSSBK(mCardInit.getAllCardInts(), mCardInit.sizeJin())) {
			item= new StandardMJPointItem();
//			item.addOpPointEnum(OpPoint.QSSL.name(), OpPoint.QSSL.value());
		}

		return item;
	}

	/**
	 * 检查风牌
	 * 
	 * @param cardList
	 * @param totalJin
	 * @return
	 */
	@Override
	public boolean checkFeng(List<Integer> cardList, int totalJin) {
		if (!CommMath.notHasSame(cardList)) {
            return false;
        }
		int sizeCard = cardList.size();
		if (sizeCard == 7) {
			return true;
		} else if (sizeCard > 8) {
			return false;
		}
		for (int i = 1; i <= totalJin; i++) {
			int sCard = sizeCard + i;
			if (sCard == 7) {
				return true;
			}
		}
		return false;

	}

	/**
	 * 检查牌间距
	 * 
	 * @param cardList
	 * @return
	 */
	@SuppressWarnings("unused")
	@Override
	public boolean checkCard(List<Integer> cardList) {
		CommMath.getSort(cardList, false);
		for (int i = 0, sizeI = cardList.size(); i < sizeI; i++) {
			for (int j = i + 1, sizeJ = cardList.size(); j < sizeJ; j++) {
				if (hunYiSe(cardList.get(i) / 10) == hunYiSe(cardList.get(j) / 10)
						&& cardList.get(i) - cardList.get(j) <= 2) {
					return false;
				}
				break;
			}
		}
		return true;
	}

	public int hunYiSe(int cardType) {
		if (MJCardCfg.WANG.value() == cardType) {
			return MJCardCfg.WANG.value();
		} else if (MJCardCfg.TIAO.value() == cardType) {
			return MJCardCfg.TIAO.value();
		} else if (MJCardCfg.TONG.value() == cardType) {
			return MJCardCfg.TONG.value();
		} else if (MJCardCfg.FENG.value() == cardType) {
			return MJCardCfg.FENG.value();
		} else {
			return MJCardCfg.NOT.value();
		}
	}

}
