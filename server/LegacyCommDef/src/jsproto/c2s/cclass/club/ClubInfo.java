package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.Player;
import jsproto.c2s.cclass.union.UnionRankingItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 俱乐部信息
 */
@Data
public class ClubInfo {
    /**
     *
     */
    Player.ShortPlayer player;

    /**
     * 俱乐部ID
     */
    private long id;
    /**
     * 随机的俱乐部标识ID
     */
    private int clubsign;
    /**
     * 俱乐部名称
     */
    private String name;
    /**
     * 俱乐部公告
     */
    private String notice;
    /**
     * 创建时间
     */
    private int creattime;
    /**
     * 房间数量
     */
    private Integer roomCount;
    /**
     * 工会ID
     */
    private long agentsID;
    /**
     * 代理等级
     */
    private int level;
    /**
     * 玩家俱乐部圈卡
     */
    private int playerClubCard = -1;
    /**
     * 等待中的房间
     */
    private Integer waitRoomCount;
    /**
     * 游戏中的房间
     */
    private Integer playingRoomCount;
    /**
     * 管理员 0:普通成员，1：管理员，2：创建者
     */
    private int minister;
    /**
     * 推广员状态(0不是推广员,1任命,2卸任)
     */
    private int promotion;

    /**
     * 推广员状态(0不是推广员,1任命,2卸任)
     */
    private int levelPromotion;
    /**
     * 人数
     */
    private int peopleNum;
    /**
     * 赛事ID
     */
    private long unionId;
    /**
     * 竞技点
     */
    private double sportsPoint;

    /**
     * 赛事名称
     */
    private String unionName;

    /**
     * 赛事职务
     */
    private Integer unionPostType;

    /**
     * 赛事标识
     */
    private Integer unionSign;
    /**
     * 存在赛事状态：0:启动,>= 1 停止
     */
    private Integer unionStateType;
    /**
     * 比赛匹配状态(0:初始状态,1:比赛进行中,2:复赛申请中,3:退赛申请中)
     */
    private Integer unionState;

    /**
     * 存在申请 1存在，0不存在
     */
    private Integer existApply;

    /**
     * 倒计时
     */
    private Integer endRoundTime;

    /**
     * 淘汰分
     */
    private Double outSportsPoint;

    /**
     * 存在新一轮通知 0不显示,1:显示通知
     */
    private Integer existRound;

    /**
     * 赛事排名项
     */
    private UnionRankingItem unionRankingItem;

    /**
     * 主办亲友圈
     */
    private String ownerClubName;

    /**
     * 排序：1：空桌子在前面
     */
    private Integer sort;

    /**
     * 加入
     * 0 需要审核 1不需要审核
     */
    private int joinNeedExamine = 0;
    /**
     * 退出
     * 0 需要审核 1不需要审核
     */
    private int quitNeedExamine = 0;
    /**
     * 俱乐部管理员钻石提醒
     */
    private int diamondsAttentionMinister = 0;
    /**
     * 俱乐部全员钻石提醒
     */
    private int diamondsAttentionAll = 0;
    /**
     * 是否是推广员管理
     */
    private int isPromotionManage = 0;
    /**
     * 是谁的推广员管理
     */
    private long promotionManagePid = 0L;
    /**
     * 桌子的数量
     */
    private int tableNum = 0;
    /**
     * 踢人（0:不允许,1:允许）
     */
    private Integer kicking;
    /**
     * 从属修改（0:不允许,1:允许）
     */
    private Integer modifyValue;
    /**
     * 显示分成（0:不允许,1:允许）
     */
    private Integer showShare;
    /**
     * 是否显示失去连接(0:正常显示,1:)
     */
    private int showLostConnect = 0;
    /**
     * 保险箱功能(0关闭、1开启
     */
    private int caseStatus = 0;
    /**
     * 邀请（0:不允许,1:允许）
     */
    private Integer invite;
    /**
     * 推广员显示列表
     */
    private List<Integer> promotionShow = new ArrayList<>();
    /**
     * 个人预警值
     */
    private Double personalSportsPointWarning;
    /**
     * 皮肤类型
     */
    private int skinType;
    /**
     * 显示上级及所属亲友圈
     */
    private int showUplevelId;
    /**
     * 显示本圈标志
     */
    private int showClubSign;
    /**
     * 赛事类型
     */
    private int unionType = 0;

    /**
     * 淘汰分
     */
    private double eliminatePoint;
    /**
     * 显示状态(是否显示成员总积分)
     */
    private int zhongZhiShowStatus;

    /**
     * 桌子类型
     */
    private int skinTable = -1;
    /**
     * 背景类型
     */
    private int skinBackColor = -1;

    /**
     * 房间游戏列表
     */
    private List<Integer> gameIdList = new ArrayList<>();
    /**
     * 房间竞技点倍数列表
     */
    private List<Double> sportsDoubleList = new ArrayList<>();

    /**
     * 对所有用户开放 0 关闭 1 开启
     */
    private int rankedOpenZhongZhi;
    /**
     * 开放入口 0 关闭 1 开启
     */
    private int rankedOpenEntryZhongZhi;
    /**
     * 圈主名字
     */
    private String clubCreateName = "";
}
