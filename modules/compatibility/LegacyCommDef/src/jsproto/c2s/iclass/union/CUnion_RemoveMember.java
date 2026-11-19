package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 联盟移除成员信息
 *
 * @author zaf
 */
@Data
public class CUnion_RemoveMember extends CUnion_Base {
    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;

}