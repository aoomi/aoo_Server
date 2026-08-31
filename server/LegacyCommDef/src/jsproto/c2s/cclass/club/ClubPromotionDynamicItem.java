package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员动态
 */
@Data
public class ClubPromotionDynamicItem {
    /**
     * 动态ID
     */
    private long id;
    /**
     * 玩家pid
     */
    private Long pid;
    /**
     * 玩家名称
     */
    private String name;
    /**
     * 执行玩家pid
     */
    private Long execPid;
    /**
     * 执行玩家名称
     */
    private String execName;
    /**
     * 执行时间
     */
    private int execTime;
    /**
     * 执行类型
     */
    private int execType;
    /**
     * 值
     */
    private String value;
    /**
     * 当前值
     */
    private String curValue;
    /**
     * 前置值
     */
    private String preValue;
    /**
     * 房间号
     */
    private String roomKey;


    public ClubPromotionDynamicItem() {
    }

    public ClubPromotionDynamicItem(long id, Long pid, String name, Long execPid, String execName, int execTime, int execType, String value, String curValue, String preValue, String roomKey) {
        this.id = id;
        this.pid = pid;
        this.name = name;
        this.execPid = execPid;
        this.execName = execName;
        this.execTime = execTime;
        this.execType = execType;
        this.value = value;
        this.curValue = curValue;
        this.preValue = preValue;
        this.roomKey = roomKey;
    }
}
