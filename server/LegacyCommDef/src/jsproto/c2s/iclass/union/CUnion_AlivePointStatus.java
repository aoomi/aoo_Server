package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 中至生存任务
 */
@Data
public class CUnion_AlivePointStatus extends CUnion_Base {


    /**
     * 页数
     */
    private int pageNum;

    /**
     * 查询的pid
     */
    private long pid;

    /**
     * 查询
     */
    private String query;
    /**
     * 查询类型
     * 0 1 2 3
     * 所有
     * 最近一天没战绩
     * 最近三天没战绩
     * 最近七天没战绩
     */
    private int type;


    public CUnion_AlivePointStatus(long unionId, long clubId, int pageNum, long pid, String query, int type) {
        super(unionId, clubId);
        this.pageNum = pageNum;
        this.pid = pid;
        this.query = query;
        this.type = type;
    }

    public CUnion_AlivePointStatus(int pageNum, long pid, String query, int type) {
        this.pageNum = pageNum;
        this.pid = pid;
        this.query = query;
        this.type = type;
    }
}
