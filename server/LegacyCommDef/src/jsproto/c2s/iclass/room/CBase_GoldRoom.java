package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 练习场的房间
 *
 * @author Huaxing
 */
public class CBase_GoldRoom extends BaseSendMsg {
    private long practiceId;

    public static CBase_GoldRoom make(long practiceId) {
        CBase_GoldRoom ret = new CBase_GoldRoom();
        ret.practiceId = practiceId;

        return ret;
    }

    public long getPracticeId() {
        return practiceId;
    }

    public void setPracticeId(long practiceId) {
        this.practiceId = practiceId;
    }


}
