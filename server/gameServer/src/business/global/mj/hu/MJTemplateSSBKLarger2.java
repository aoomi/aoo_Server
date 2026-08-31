package business.global.mj.hu;

import com.ddm.server.common.utils.CommMath;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定义2：玩家手中序数牌点数间隔必须>2，不能靠牌或者重复，字牌不重复；
 * 可以是147、148、149、158、159、169/258、259、269、369；
 * 字牌不重复，不当顺子；
 * 可选：组成烂胡的数字牌不能出现相同的值，如不能出现2筒跟2万同时存在；
 */
public class MJTemplateSSBKLarger2 extends MJTemplateSSBKRandom14 {

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

    /**
     * 检查间距
     *
     * @param large
     * @param min
     * @return
     */
    @Override
    public boolean checkNotInSpace(Integer large, Integer min) {
        return large / 10 == min / 10 && large - min <= 2;
    }
}
