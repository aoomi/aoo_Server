package business.global.mj.manage;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;

import java.util.List;

public interface TingCard {

    /**
     * 检查枪金听牌列表
     *
     * @param mSetPos 玩家信息
     * @param idx     下标位置
     * @param isDPos  是否庄家
     * @return
     */
    public List<MJCard> qangJinTingList(AbsMJSetPos mSetPos, int idx, boolean isDPos);

    /**
     * 检查听牌
     *
     * @param mInit
     * @param Jins
     * @return
     */
    public List<Integer> checkTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList);

    /**
     * 检查是否只听金牌
     *
     * @param mInit
     * @param Jins
     * @return
     */
    public List<Integer> tingJinCard(AbsMJSetPos mSetPos, List<MJCard> allCardList);

    /**
     * 检查听列表
     *
     * @param mSetPos
     * @return
     */
    public boolean checkTingList(AbsMJSetPos mSetPos);
}
