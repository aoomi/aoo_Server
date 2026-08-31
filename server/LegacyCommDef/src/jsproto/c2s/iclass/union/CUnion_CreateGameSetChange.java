package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 获取赛事游戏设置
 *
 * @author zaf
 */
@Data
public class CUnion_CreateGameSetChange extends CUnion_Base {

    /**
     * 房间配置Id
     */
    private int unionRoomCfgId;
    /**
     * 状态
     */
    private int status;

}