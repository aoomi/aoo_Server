package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_MemberList extends CUnion_Base {
    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 查询内容
     */
    private String query;
    /**
     * 查询日期
     * <p>
     * 0 -6
     * 今天到前七天
     */
    private int type;


}