package business.global.mj.manage;

import business.global.mj.AbsMJSetPos;

public interface OpCard {

    /**
     * 检查操作的牌
     *
     * @param mSetPos 玩家信息
     * @param cardID  牌ID
     * @return
     */
    public boolean checkOpCard(AbsMJSetPos mSetPos, int cardID);

    /**
     * 点击操作的牌
     *
     * @param mSetPos 玩家信息
     * @param cardID  牌ID
     * @return
     */
    public boolean doOpCard(AbsMJSetPos mSetPos, int cardID);
}
