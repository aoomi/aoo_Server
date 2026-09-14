package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取联盟成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_MinisterZhongZhiSave extends CUnion_Base {
    /**
     * 设置的俱乐部列表（被管理的俱乐部列表）
     */
    private List<Long> clubIdList=new ArrayList<>();
    /**
     * 成为管理的俱乐部id
     */
    private long opClubId;


}