package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * @author zaf
 */
@Data
public class CUnion_RoomCfgUpdate extends CUnion_Base {
    /**
     * 房间配置Id
     */
    private int unionRoomCfgId;
    /**
     * 更新状态
     */
    private int status;
}