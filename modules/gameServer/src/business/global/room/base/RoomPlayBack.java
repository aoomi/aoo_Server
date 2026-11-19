package business.global.room.base;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.playback.PlayBackData;

import java.util.List;

public interface RoomPlayBack {
    /**
     * 添加回放记录
     *
     * @param playBackData
     */
    public void addPlayBack(PlayBackData playBackData);


    /**
     * @param msg
     * @param setPosCard
     */
    public <T> void playBack2All(BaseSendMsg msg);

    /**
     * @param pos
     * @param msg
     * @param setPosCard
     */
    public <T> void playBack2Pos(int pos, BaseSendMsg msg, List<T> setPosCard);

    /**
     * @param msg
     * @param setPosCard
     */
    public <T> void addPlaybackList(BaseSendMsg msg, List<T> setPosCard);


    public void clear();
}
