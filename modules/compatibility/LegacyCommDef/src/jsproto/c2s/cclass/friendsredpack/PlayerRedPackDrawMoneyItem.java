package jsproto.c2s.cclass.friendsredpack;


public class PlayerRedPackDrawMoneyItem {
    private int getTime;
    private int value;


    public PlayerRedPackDrawMoneyItem(int getTime, int value) {
        super();
        this.getTime = getTime;
        this.value = value;
    }

    public int getGetTime() {
        return getTime;
    }

    public void setGetTime(int getTime) {
        this.getTime = getTime;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
