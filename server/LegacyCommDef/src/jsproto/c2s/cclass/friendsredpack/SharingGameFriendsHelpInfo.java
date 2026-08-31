package jsproto.c2s.cclass.friendsredpack;

/**
 * 分享游戏给好友帮助拆红包
 *
 * @author Administrator
 */
public class SharingGameFriendsHelpInfo {
    // 已拆得红包
    private int dismantleValue;
    // 再拆得糊红包
    private int againDismantleValue;


    public SharingGameFriendsHelpInfo(int dismantleValue, int againDismantleValue) {
        super();
        this.dismantleValue = dismantleValue;
        this.againDismantleValue = againDismantleValue;
    }

    public int getDismantleValue() {
        return dismantleValue;
    }

    public void setDismantleValue(int dismantleValue) {
        this.dismantleValue = dismantleValue;
    }

    public int getAgainDismantleValue() {
        return againDismantleValue;
    }

    public void setAgainDismantleValue(int againDismantleValue) {
        this.againDismantleValue = againDismantleValue;
    }


}
