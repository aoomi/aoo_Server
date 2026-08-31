package business.global.mj.hu;

import com.ddm.server.common.utils.CommMath;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定义3：序数牌必须为147,258,369一组一色，外加七个字牌中的五个字；（参考四底翻精）
 * 例：147万+258条+369筒+东南西北中发白中不重复的五个字；
 * 例：147条+258万+369筒+东南西北中发白中不重复的五个字；
 * 万筒条都要有且必须是147 258 369这种数列；
 */
public class MJTemplateSSBK5Zi extends MJTemplateSSBKRandom14 {


    /**
     * 检查是否都是唯一风牌
     * ，外加七个字牌中的五个字；（参考四底翻精）
     *
     * @param cardList
     * @param totalJin
     * @return
     */
    @Override
    public boolean checkFeng(List<Integer> cardList, int totalJin) {
        if (CommMath.hasSame(cardList)) {
            return false;
        }
        if (cardList.stream().anyMatch(k -> k > 47 || k < 40)) {
            return false;
        }
        return cardList.size() == 5;
    }

    /**
     * 检查牌间距
     * 可选：组成烂胡的数字牌不能出现相同的值，如不能出现2筒跟2万同时存在；
     *
     * @param cardList
     * @return
     */
    @Override
    public boolean checkCard(List<Integer> cardList) {
        //可选：组成烂胡的数字牌不能出现相同的值，如不能出现2筒跟2万同时存在；
        List<Integer> checkSameList = cardList.stream().map(k -> k % 10).collect(Collectors.toList());
        if (CommMath.hasSame(checkSameList)) {
            return false;
        }
        return super.checkCard(cardList);
    }

}
