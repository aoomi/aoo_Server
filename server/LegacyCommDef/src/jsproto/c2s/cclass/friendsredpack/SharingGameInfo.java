package jsproto.c2s.cclass.friendsredpack;

import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 分享游戏信息
 *
 * @author Administrator
 */
public class SharingGameInfo {
    // 玩家信息
    private ShortPlayer player;
    // 总元 totalValue
    private int totalValue;
    // 获得 dismantleValue
    private int dismantleValue;


    public SharingGameInfo(ShortPlayer player, int totalValue, int dismantleValue) {
        super();
        this.player = player;
        this.totalValue = totalValue;
        this.dismantleValue = dismantleValue;
    }

    public ShortPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ShortPlayer player) {
        this.player = player;
    }

    public int getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(int totalValue) {
        this.totalValue = totalValue;
    }

    public int getDismantleValue() {
        return dismantleValue;
    }

    public void setDismantleValue(int dismantleValue) {
        this.dismantleValue = dismantleValue;
    }


}
