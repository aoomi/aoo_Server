package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:57
 * @description 共享竞技场房间配置
 */
@Data
public class ShareArenaRoomConfig {
    // 赛场ID
    private long aid = 0L;
    // 赛场名称
    private String arenaName;
    // 赛制状态 1.定局积分赛(默认)2.打立出局赛.3.瑞士移位赛
    private int formatType;
    // 赛制阶段配置
    private Object cfgInfo;
    // 初始分数
    private int initPoint = 0;
    private int baseMark; // 基础分
}
