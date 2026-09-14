package jsproto.c2s.cclass.friendsredpack;

import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 玩家红包记录项
 *
 * @author Administrator
 */
public class PlayerRedPackRecordItem {
    private ShortPlayer player;
    private boolean isHu;
    private boolean isOwner;
    private int value;

    public PlayerRedPackRecordItem(ShortPlayer player, boolean isHu, boolean isOwner, int value) {
        super();
        this.player = player;
        this.isHu = isHu;
        this.isOwner = isOwner;
        this.value = value;
    }

    public ShortPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ShortPlayer player) {
        this.player = player;
    }

    public boolean isHu() {
        return isHu;
    }

    public void setHu(boolean isHu) {
        this.isHu = isHu;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
