package jsproto.c2s.cclass.playback;

public class PlayerPlayBackCodeBO {
    private long id;
    private int playBackCode = 0;

    public static String getItemsName() {
        return "id,playBackCode";
    }

    public static String getItemsNameBackCode() {
        return "playBackCode";
    }

    public int getPlayBackCode() {
        return playBackCode;
    }

    public void setPlayBackCode(int playBackCode) {
        this.playBackCode = playBackCode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}