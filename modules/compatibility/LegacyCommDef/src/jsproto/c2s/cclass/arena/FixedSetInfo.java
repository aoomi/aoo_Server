package jsproto.c2s.cclass.arena;

/**
 * 定局积分赛
 *
 * @author Administrator
 */
public class FixedSetInfo {
    private int setCount = 1;//局数
    private int round = 0;//轮数

    public int getSetCount() {
        return setCount;
    }

    public void setSetCount(int setCount) {
        this.setCount = setCount;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }


}
