package jsproto.c2s.cclass.friendsredpack;

import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 提款信息
 *
 * @author Administrator
 */
public class DrawMoneyInfo {
    // 玩家信息
    private ShortPlayer player;
    // 已拆的红包
    private int dismantleValue;
    // 好友胡牌可领取数
    private int friendHuPaiCount;
    // 无门槛红包数
    private int noThresholdCount;

    public DrawMoneyInfo(ShortPlayer player, int dismantleValue, int friendHuPaiCount, int noThresholdCount) {
        super();
        this.player = player;
        this.dismantleValue = dismantleValue;
        this.friendHuPaiCount = friendHuPaiCount;
        this.noThresholdCount = noThresholdCount;
    }

    public ShortPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ShortPlayer player) {
        this.player = player;
    }

    public int getDismantleValue() {
        return dismantleValue;
    }

    public void setDismantleValue(int dismantleValue) {
        this.dismantleValue = dismantleValue;
    }

    public int getFriendHuPaiCount() {
        return friendHuPaiCount;
    }

    public void setFriendHuPaiCount(int friendHuPaiCount) {
        this.friendHuPaiCount = friendHuPaiCount;
    }

    public int getNoThresholdCount() {
        return noThresholdCount;
    }

    public void setNoThresholdCount(int noThresholdCount) {
        this.noThresholdCount = noThresholdCount;
    }


}
