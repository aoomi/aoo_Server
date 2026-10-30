package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * @author zaf
 */
@Data
public class CUnion_Dynamic extends CUnion_Base {
    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 获取时间 0今天,1昨天,2最近三天,3近30天
     */
    private int getType;
    /**
     * 查询Pid
     */
    private long pid;

    /**
     * 操作者Pid
     */
    private long execPid;
}