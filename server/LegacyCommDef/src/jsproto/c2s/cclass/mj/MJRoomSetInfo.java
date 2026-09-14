package jsproto.c2s.cclass.mj;

import cenum.room.SetState;
import jsproto.c2s.cclass.room.RoomSetInfo;

import java.util.ArrayList;
import java.util.List;

// 一局信息
public class MJRoomSetInfo extends RoomSetInfo {
    // 庄家位置
    private int dPos = 0;
    // 骰子
    private List<Integer> saizi = new ArrayList<>();
    // 开始拿牌的位置
    private int startPaiPos = 0;
    // 开始拿牌的蹲位
    private int startPaiDun = 0;
    // 普通摸牌数量
    private int normalMoCnt = 0;
    // 杠后摸牌数量
    private int gangMoCnt = 0;
    // 最后操作时间
    private int lastShotTime = 0;
    // 当前时间
    private int setCurrentTime;
    // 最近一个出牌，等待被人操作接手的CardID， 用于箭头标识
    private int waitReciveCard = 0;
    // 每个玩家的牌面
    private List<BaseMJSet_Pos> setPosList = new ArrayList<>();
    // 当前局状态 Init；Wait；End； Playing不需要信息
    private SetState state = SetState.Init;
    // 当前等待信息 Wait
    private BaseMJRoom_SetRound setRound = new BaseMJRoom_SetRound();
    // 结束状态
    private BaseMJRoom_SetEnd setEnd = new BaseMJRoom_SetEnd();

    public int getdPos() {
        return dPos;
    }

    public void setdPos(int dPos) {
        this.dPos = dPos;
    }


    public int getLastShotTime() {
        return lastShotTime;
    }

    public void setLastShotTime(int lastShotTime) {
        this.lastShotTime = lastShotTime;
    }

    public int getSetCurrentTime() {
        return setCurrentTime;
    }

    public void setSetCurrentTime(int setCurrentTime) {
        this.setCurrentTime = setCurrentTime;
    }

    public List<Integer> getSaizi() {
        return saizi;
    }

    public void setSaizi(List<Integer> saizi) {
        this.saizi = saizi;
    }

    public int getStartPaiPos() {
        return startPaiPos;
    }

    public void setStartPaiPos(int startPaiPos) {
        this.startPaiPos = startPaiPos;
    }

    public int getStartPaiDun() {
        return startPaiDun;
    }

    public void setStartPaiDun(int startPaiDun) {
        this.startPaiDun = startPaiDun;
    }

    public int getNormalMoCnt() {
        return normalMoCnt;
    }

    public void setNormalMoCnt(int normalMoCnt) {
        this.normalMoCnt = normalMoCnt;
    }

    public int getGangMoCnt() {
        return gangMoCnt;
    }

    public void setGangMoCnt(int gangMoCnt) {
        this.gangMoCnt = gangMoCnt;
    }

    public int getWaitReciveCard() {
        return waitReciveCard;
    }

    public void setWaitReciveCard(int waitReciveCard) {
        this.waitReciveCard = waitReciveCard;
    }

    public List<BaseMJSet_Pos> getSetPosList() {
        return setPosList;
    }

    public void setSetPosList(List<BaseMJSet_Pos> setPosList) {
        this.setPosList = setPosList;
    }

    public void addSetPosList(BaseMJSet_Pos setPos) {
        this.setPosList.add(setPos);
    }

    public SetState getState() {
        return state;
    }

    public void setState(SetState state) {
        this.state = state;
    }

    public BaseMJRoom_SetRound getSetRound() {
        return setRound;
    }

    public void setSetRound(BaseMJRoom_SetRound setRound) {
        this.setRound = setRound;
    }

    public BaseMJRoom_SetEnd getSetEnd() {
        return setEnd;
    }

    public void setSetEnd(BaseMJRoom_SetEnd setEnd) {
        this.setEnd = setEnd;
    }

}
