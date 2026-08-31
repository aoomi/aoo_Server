package jsproto.c2s.iclass.club;

import jsproto.c2s.iclass.union.CUnion_Base;
import lombok.Data;

/**
 * 禁止亲友圈成员游戏
 *
 * @author zaf
 */
@Data
public class CClub_BanGamePlayer  {
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
    /**
     * 亲友圈Id
     */
    private long clubId;

}