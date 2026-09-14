package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:56
 * @description 共享机器人房间配置
 */
@Data
public class ShareRobotRoomConfig {
    // 基础分
    private int baseMark;
    // 最小值
    private int min;
    // 最大值
    private int max;
    // 练习场ID
    private long practiceId = -1;
}
