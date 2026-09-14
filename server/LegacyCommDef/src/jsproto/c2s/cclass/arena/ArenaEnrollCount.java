package jsproto.c2s.cclass.arena;

/**
 * 每日或者每周或者无限次的已报名统计，每个赛场只有一个该对象
 */
public class ArenaEnrollCount {
    private int count = 0;//报名人数
    private int enrollTime = 0;//报名时间，如果按周，一周内就只有一个值，并且是首次创建时间戳，如果按日，每天都会有每天的时间戳值

    public ArenaEnrollCount() {
        super();
    }

    public ArenaEnrollCount(int count, int enrollTime) {
        super();
        this.count = count;
        this.enrollTime = enrollTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(int enrollTime) {
        this.enrollTime = enrollTime;
    }

}
