package jsproto.c2s.cclass.mj;

import cenum.mj.HuType;
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
public class BaseMJRoom_PosEnd<T> {
    private int pos = 0; // 位置
    private long pid = 0L;
    private HuType huType = HuType.NotHu; // 每个玩家的胡牌类型 0不胡 ；1自摸；2抢杠胡
    private boolean isJiePao = false; // 是否接炮
    private boolean isReward = false;
    private int point = 0; // 本局积分变更
    private Double sportsPoint;
    private Double roomSportsPoint;
    private int roomPoint = 0;
    private T endPoint;
    private List<Integer> shouCard = new ArrayList<>(); //
    private int handCard = 0; //
    private List<List<Integer>> publicCardList = new ArrayList<List<Integer>>();
    private List<Integer> huaList = new ArrayList<Integer>();
    public List<List<Integer>> kouPaisList = new ArrayList<>();
    public int zhongMaCount = -1; //默认-1，代表没抓花
    private Long upLevelId;//上级推广员id

}
