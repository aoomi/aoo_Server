package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_MemberExamineList extends CUnion_Base {
    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 0:加入审核,否则退出审核
     */
    private int type = 0;
    /**
     * 查询条件
     */
    private String query = "";


}