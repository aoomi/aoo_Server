package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 联赛创建
 *
 * @author
 */
@Data
public class CUnion_Create extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 名字
     */
    private String unionName;
    /**
     * 加入申请(0需要审核、1不需要审核)
     */
    private int join;
    /**
     * 退出申请(0需要审核、1不需要审核)
     */
    private int quit;
    /**
     * 裁判力度
     */
    private double initSports;
    /**
     * 比赛频率（30天，7天，每天）
     */
    private int matchRate;
    /**
     * 赛事淘汰
     */
    private double outSports;
    /**
     * 消耗类型(1-金币,2-房卡)
     */
    private int prizeType = 1;
    /**
     * 排名前50名
     */
    private int ranking;
    /**
     * 数量
     */
    private int value;
}