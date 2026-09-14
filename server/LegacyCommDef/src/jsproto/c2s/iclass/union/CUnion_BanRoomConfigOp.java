package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 联盟禁止成员指定游戏配置
 */
@Data
public class CUnion_BanRoomConfigOp extends CUnion_Base {
    /**
     * 配置Id列表
     */
    private List<Long> configIdList = Collections.emptyList();
    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;
    /**
     * 是否全选 0:不,1:全选
     */
    private int isAll;

}
