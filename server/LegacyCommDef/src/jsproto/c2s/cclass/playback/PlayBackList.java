package jsproto.c2s.cclass.playback;

import java.util.List;


public class PlayBackList {
    private List<PlayBackMsg> playbackList;

    public PlayBackList(List<PlayBackMsg> list) {
        super();
        this.playbackList = list;
    }

    public PlayBackList() {
        super();
    }

    public List<PlayBackMsg> getPlaybackList() {
        return playbackList;
    }

    public void setPlaybackList(List<PlayBackMsg> playbackList) {
        this.playbackList = playbackList;
    }

}
