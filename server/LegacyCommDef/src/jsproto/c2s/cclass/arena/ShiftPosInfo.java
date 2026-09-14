package jsproto.c2s.cclass.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * 瑞士移位的简单的定义：每桌打一定副数之后，所有参赛选手的分数会综合在一起排名，按照这个排名决定下一轮的对手。 排名：全体排名，每轮结束重排
 * 淘汰：每次组桌前淘汰一部分人（或晋级多少人），淘汰之后才会重新组桌。这个淘汰可以按固定人数（如20人）或者按比例（如20%） 如果淘汰的话，淘汰次数 =
 * 轮次 - 1，因为一上来就开始打的这第一桌并没有淘汰过程。 不同赛场的可配置；
 * 组桌：按排名把淘汰后的人组桌。一般有两种方法：一种是按照排名顺序组桌，强对强弱对弱，如1234名一桌，5678名一桌；还有一种是高分和低分在一桌，
 * 保护高分选手； 轮次：顾名思义就是需要打几轮，也就是需要排名组桌的次数 不同赛场的可配置；
 * 局数：每次组桌之后需要打几副牌，也就是你再同一张桌上需要玩几把牌 不同赛场的可配置； 初始分：比赛开始第一局玩家所带的积分； 不同赛场的可配置；
 * 轮间带分：从上一轮中可以带下来的分数，正常为上一轮的积分*系数； 不同赛场的可配置；
 * {"outRatio":10,"round":3,"setNum":2,"initPoint":100,"ratioPoint":0.3}
 *
 * @author Administrator
 **/
public class ShiftPosInfo {
    // 淘汰比率
    private List<ProRatio> proRatioList = new ArrayList<>();
    // 轮次
    private int round = 0;
    // 局数
    private int setNum = 0;
    // 初始分
    private int initPoint = 0;
    // 轮间带分
    private double ratioPoint = 0;

    public List<ProRatio> getProRatioList() {
        return proRatioList;
    }

    public void setProRatioList(List<ProRatio> proRatioList) {
        this.proRatioList = proRatioList;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getSetNum() {
        return setNum;
    }

    public void setSetNum(int setNum) {
        this.setNum = setNum;
    }

    public int getInitPoint() {
        return initPoint;
    }

    public void setInitPoint(int initPoint) {
        this.initPoint = initPoint;
    }

    public int getRatioPoint(int initPoint) {
        double rPoint = ratioPoint * initPoint;
        int gPoint = (int) Math.round(rPoint);
        return gPoint;
    }

    public void setRatioPoint(double ratioPoint) {
        this.ratioPoint = ratioPoint;
    }

}
