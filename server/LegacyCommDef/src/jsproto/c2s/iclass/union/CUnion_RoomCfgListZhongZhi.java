package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_RoomCfgListZhongZhi extends CUnion_Base {
    /**
     * 第几页
     */
    private int pageNum;

    /**
     * 类型
     */
    private int classType = 0;
    /**
     * 玩家id
     */
    private long pid;


}