package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事房间玩法项
 */
@Data
public class UnionRoomPromotionShareCfgItem {
    /**
     * id
     */
    private long id;
    /**
     * 赛事id
     */
    private long unionId;
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 玩家pid
     */
    private long pid;
    /**
     * 房间配置id
     */
    private long configId;
    /**
     * 更新时间
     */
    private int updateTime;
    /**
     * 创建时间
     */
    private int createTime;
    /**
     * 积分比例
     */
    private double scorePercent;
    /**
     * 分数分成值
     */
    private double scoreDividedInto;
    /**
     * 类型（0：百分比，1：固定值）
     */
    private int type;

    public static String getItemsNameCount() {
        return "id,unionId,clubId,pid,configId,updateTime,createTime,scorePercent,scoreDividedInto,type";
    }

}
