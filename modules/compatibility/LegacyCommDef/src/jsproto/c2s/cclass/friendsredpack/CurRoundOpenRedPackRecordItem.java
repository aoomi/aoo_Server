package jsproto.c2s.cclass.friendsredpack;

import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 当前回合（本轮）打开红包记录项
 *
 * @author Administrator
 */
public class CurRoundOpenRedPackRecordItem {
    private ShortPlayer player;
    private int getTime;
    private String reward;
    private int value;


    public CurRoundOpenRedPackRecordItem(ShortPlayer player, int getTime, String reward, int value) {
        super();
        this.player = player;
        this.getTime = getTime;
        this.reward = reward;
        this.value = value;
    }

    public ShortPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ShortPlayer player) {
        this.player = player;
    }

    public int getGetTime() {
        return getTime;
    }

    public void setGetTime(int getTime) {
        this.getTime = getTime;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public int getVlaue() {
        return value;
    }

    public void setVlaue(int value) {
        this.value = value;
    }


}
