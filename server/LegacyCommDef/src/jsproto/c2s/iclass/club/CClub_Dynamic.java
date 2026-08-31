package jsproto.c2s.iclass.club;

import lombok.Data;

@Data
public class CClub_Dynamic {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 获取时间 0今天,1昨天,2最近三天,3近30天
     */
    private int getType;
    /**
     * 被操作Pid
     */
    private long pid;

    /**
     * 操作者Pid
     */
    private long execPid;
    /**
     * 选择的分类
     * 0 全部
     * 1 异常操作
     * 2 对局输赢
     * 3 报名费
     */
    private int chooseType;

}
