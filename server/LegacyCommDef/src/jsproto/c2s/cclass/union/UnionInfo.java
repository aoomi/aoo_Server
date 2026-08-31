package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionInfo {
    /**
     * 赛事id
     */
    private long unionId;
    /**
     * 赛事名称
     */
    private String unionName;
    /**
     * 赛事职位
     */
    private Integer unionPostType;

    /**
     * 赛事标识
     */
    private Integer unionSign;

    /**
     * 倒计时
     */
    private Integer endRoundTime;

    /**
     * 淘汰分
     */
    private Double outSportsPoint;

    /**
     * 回合id
     */
    private int roundId;

    /**
     * 主办亲友圈
     */
    private String ownerClubName;

    /**
     * 存在赛事状态：0:启动,>= 1 停止
     */
    private int unionStateType;

    /**
     * 排序
     */
    private int sort;
    /**
     * 桌子的数量
     */
    private int tableNum;
    /**
     * 是否显示失去连接(0:根据状态显示,1:所有人显示在线状态)
     */
    private int showLostConnect = 0;
    /**
     * 保险箱功能(0关闭、1开启
     */
    private int caseStatus = 0;
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
     * 显示状态(是否显示成员总积分)
     */
    private int zhongZhiShowStatus;
    /**
     * 桌子类型
     */
    private int skinTable;
    /**
     * 背景类型
     */
    private int skinBackColor;
    /**
     * 对所有用户开放 0 关闭 1 开启
     */
    private int rankedOpenZhongZhi;
    /**
     * 开放入口 0 关闭 1 开启
     */
    private int rankedOpenEntryZhongZhi;

    public UnionInfo(long unionId, String unionName, Integer unionPostType, Integer unionSign, Integer endRoundTime,
                     Double outSportsPoint, int roundId, String ownerClubName, int unionStateType, int sort, int tableNum,
                     int showLostConnect, int caseStatus, int showUplevelId, int showClubSign, int unionType, int zhongZhiShowStatus, int skinTable, int skinBackColor, int rankedOpenZhongZhi, int rankedOpenEntryZhongZhi) {
        this.unionId = unionId;
        this.unionName = unionName;
        this.unionPostType = unionPostType;
        this.unionSign = unionSign;
        this.endRoundTime = endRoundTime;
        this.outSportsPoint = outSportsPoint;
        this.roundId = roundId;
        this.ownerClubName = ownerClubName;
        this.unionStateType = unionStateType;
        this.sort = sort;
        this.tableNum = tableNum;
        this.showLostConnect = showLostConnect;
        this.caseStatus = caseStatus;
        this.showUplevelId = showUplevelId;
        this.showClubSign = showClubSign;
        this.unionType = unionType;
        this.zhongZhiShowStatus = zhongZhiShowStatus;
        this.skinTable = skinTable;
        this.skinBackColor = skinBackColor;
        this.rankedOpenZhongZhi = rankedOpenZhongZhi;
        this.rankedOpenEntryZhongZhi = rankedOpenEntryZhongZhi;
    }
}
