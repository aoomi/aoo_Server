package jsproto.c2s.cclass.pk;

import lombok.Data;

/**
 * 龙岩麻将 配置
 *
 * @author Clark
 */

// 位置结束的信息
@Data
public class BasePKRoom_ActualPos {
    /**
     * 玩家Pid
     */
    private long pid = 0L;
    /**
     * 本局积分变更
     */
    private int point = 0;

    private String nickname;

    /**
     * 房间分数
     */
    private int roomPoint = 0;

}
