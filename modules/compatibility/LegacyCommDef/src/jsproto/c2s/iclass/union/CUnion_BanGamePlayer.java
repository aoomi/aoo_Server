package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止俱乐部成员游戏
 *
 * @author zaf
 */
@Data
public class CUnion_BanGamePlayer extends CUnion_Base {
    /**
     * 查询内容
     */
    private String query;
    /**
     * 页数
     */
    private int pageNum;
    /**
     * 要禁止的成员pid
     */
    private long pid;


}