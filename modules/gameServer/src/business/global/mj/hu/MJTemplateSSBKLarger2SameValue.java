package business.global.mj.hu;

/**
 * 定义2：玩家手中序数牌点数间隔必须>2，字牌不重复；
 * 《烂胡的数字牌可以出现相同的值》
 */
public class MJTemplateSSBKLarger2SameValue extends MJTemplateSSBKRandom14 {

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
