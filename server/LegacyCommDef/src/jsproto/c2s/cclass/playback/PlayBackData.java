package jsproto.c2s.cclass.playback;

import lombok.Data;

@Data
public class PlayBackData {
    private long roomID;
    private int setID;
    private int dPos;            //庄家
    private int count;
    private String roomKey;
    private int gameType;
    private PlayBackDateTimeInfo playBackDateTimeInfo;

    public PlayBackData(long roomID, int setID, int dPos, int count,
                        String roomKey, int gameType, PlayBackDateTimeInfo playBackDateTimeInfo) {
        super();
        this.roomID = roomID;
        this.setID = setID;
        this.dPos = dPos;
        this.count = count;
        this.roomKey = roomKey;
        this.gameType = gameType;
        this.playBackDateTimeInfo = playBackDateTimeInfo;
    }
}
