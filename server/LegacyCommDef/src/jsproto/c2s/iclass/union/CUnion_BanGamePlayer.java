package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 禁止亲友圈成员游戏
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
     * 要禁止的玩家pid
     */
    private long pid;


}