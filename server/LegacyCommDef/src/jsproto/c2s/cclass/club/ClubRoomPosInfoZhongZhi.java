package jsproto.c2s.cclass.club;


import lombok.Data;

/**
 * @author FengZhnag
 * @date 2022/9/13 9:37
 * @description 中至推广员分成竞技动态记录信息
 */
@Data
public class ClubRoomPosInfoZhongZhi {
    /**
     * 玩家pid
     */
    private long pid;
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 是否大赢家
     */
    private boolean isWinner;
    /**
     *  玩家报名费（大赢家或者平分付存的是转换过的负值）
     */
    private  double roomSportsConsume;
    /**
     * 玩家输赢分（比赛分）
     */
    private  double winLosePoint;
    /**
     * 玩家分成分（分成过程产生的分数）
     */
    private  double promotionPoint;

    public ClubRoomPosInfoZhongZhi() {
    }

    public ClubRoomPosInfoZhongZhi(long pid, long clubId, boolean isWinner, double consume, double winLosePoint) {
        this.pid = pid;
        this.clubId = clubId;
        this.isWinner = isWinner;
        this.roomSportsConsume = consume;
        this.winLosePoint = winLosePoint;
    }

    /**
     * 没有参与游戏人的构造方法
     * @param pid
     * @param clubId
     * @param isWinner
     * @param consume
     * @param winLosePoint
     * @param promotionPoint
     */
    public ClubRoomPosInfoZhongZhi(long pid, long clubId, boolean isWinner, double consume, double winLosePoint,double promotionPoint) {
        this.pid = pid;
        this.clubId = clubId;
        this.isWinner = isWinner;
        this.roomSportsConsume = consume;
        this.winLosePoint = winLosePoint;
        this.promotionPoint = promotionPoint;
    }
}
