package jsproto.c2s.cclass.pk.base;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 龙岩麻将 配置
 *
 * @param <T>
 * @author Clark
 */

// 位置结束的信息
@Data
public class BasePKRoom_PosEnd<T> {
    /**
     * 位置
     */
    private int pos = 0;
    /**
     * 玩家Pid
     */
    private long pid = 0L;
    /**
     * 是否奖励
     */
    private boolean isReward = false;
    /**
     * 本局积分变更
     */
    private int point = 0;
    /**
     * 比赛分
     */
    private Double sportsPoint;

    private String nickname;

    /**
     * 房间分数
     */
    private int roomPoint = 0;
    private T endPoint;
    private List<Integer> shouCard = new ArrayList<>(); //
    private Integer handCard = 0; //
    private List<List<Integer>> publicCardList = new ArrayList<>();//公共牌
    private Long upLevelId;//上级推广员id

}
