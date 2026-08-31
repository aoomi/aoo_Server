package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
public class CClub_SetConfig extends BaseSendMsg {
    private long clubId;
    // 基础设置
    private List<Integer> basics = new ArrayList<>();
    // 提出房间
    private int kickOutRoom;
    // 解散设置
    private int dissolveSet;
    // 解散时间
    private int dissolveTime;

    public long getClubID() {
        return clubId;
    }

    public List<Integer> getBasics() {
        return basics;
    }

    public void setBasics(List<Integer> basics) {
        this.basics = basics;
    }

    public int getKickOutRoom() {
        return kickOutRoom;
    }

    public void setKickOutRoom(int kickOutRoom) {
        this.kickOutRoom = kickOutRoom;
    }

    public int getDissolveSet() {
        return dissolveSet;
    }

    public void setDissolveSet(int dissolveSet) {
        this.dissolveSet = dissolveSet;
    }

    public int getDissolveTime() {
        return dissolveTime;
    }

    public void setDissolveTime(int dissolveTime) {
        this.dissolveTime = dissolveTime;
    }
}
