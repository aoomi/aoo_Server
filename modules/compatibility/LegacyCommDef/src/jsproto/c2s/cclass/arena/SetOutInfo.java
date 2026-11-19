package jsproto.c2s.cclass.arena;

/**
 * 打立出局信息
 * <p>
 * 1、开赛人数可以设置一个范围(必须是椅子数的整数倍，达到上限人数或者满足下限人数时，且报名时间到后开赛;如果没有达到上限人数，后台可以设置延迟一定时间，让更多的人报名);
 * 2、比赛开始，先进行淘汰赛阶段：参赛玩家每人携带系统给定的初始积分进行比赛，打完一局重新配桌，如果某个玩家的积分小于设定的淘汰分即所谓的“打立”，该玩家就被淘汰(初始积分、
 * 淘汰分可以后台设置)，如果比赛过程中掉线，在淘汰赛阶段结束前没有返回赛场也会被淘汰;
 * 3、定局积分赛阶段：淘汰剩余人数达到设定值则停止淘汰，并按照积分从高到低筛选一定数目的玩家进入定局赛阶段(后台可设，人数需要满足定局赛的要求人数=C(一张桌子坐满人的数目)*2n，
 * 不能大于报名下限人数)，同时玩家的分数也会重置，带分=(当前积分开根号)* m(m可设定，一般设置为0.1);
 * 4、玩家获奖规则与定局积分赛一样。
 * 5、ASS赛制是为了避免在比赛前期打立出局制时基数增长缓慢造成积分高的一桌比赛时间延长。所以设置了一些参数，其中包含系统设置每轮固定局数。例如设定局数为3轮，如果3局之后此桌
 * 没有人被打立淘汰，则系统开始将此3人重新分桌，如果在3局内有玩家被打立，则系统直接分桌。（此赛制只使用在打立出局阶段）
 * <p>
 * 可配置内容：
 * 初始分；
 * 	淘汰分；
 * 	淘汰剩余人数；
 * 	晋级定局赛人数；
 * 	淘汰阶段一桌几局；
 * 	淘汰赛进入定局赛的带分系数；
 * 	定局赛阶段游戏局数；
 *
 * @author Administrator
 */
public class SetOutInfo {

    //	初始分；
    private int initPoint = 0;
    //	淘汰分；
    private int outPoint = 0;
    //	淘汰剩余人数；
    private int outNum = 0;
    //	晋级定局赛人数；
    private int proNum = 0;
    //	淘汰阶段一桌几局；
    private int outSet = 0;
    //	淘汰赛进入定局赛的带分系数；
    private double ratioPoint = 0;
    // 定局赛阶段游戏局数；
    private int proSet = 0;
    // 轮数
    private int proRound = 0;
    //	开根号Math.sqrt(3)


    public int getInitPoint() {
        return initPoint;
    }

    public void setInitPoint(int initPoint) {
        this.initPoint = initPoint;
    }

    public int getOutPoint() {
        return outPoint;
    }

    public void setOutPoint(int outPoint) {
        this.outPoint = outPoint;
    }

    public int getOutNum() {
        return outNum;
    }

    public void setOutNum(int outNum) {
        this.outNum = outNum;
    }

    public int getProNum() {
        return proNum;
    }

    public void setProNum(int proNum) {
        this.proNum = proNum;
    }

    public int getOutSet() {
        return outSet;
    }

    public void setOutSet(int outSet) {
        this.outSet = outSet;
    }

    public int getRatioPoint(int initPoint) {
        double rPoint = Math.sqrt(initPoint) * this.ratioPoint;
        int gPoint = (int) Math.round(rPoint);
        return gPoint;
    }

    public void setRatioPoint(double ratioPoint) {
        this.ratioPoint = ratioPoint;
    }

    public int getProSet() {
        return proSet;
    }

    public void setProSet(int proSet) {
        this.proSet = proSet;
    }

    public int getProRound() {
        return proRound;
    }

    public void setProRound(int proRound) {
        this.proRound = proRound;
    }
}
