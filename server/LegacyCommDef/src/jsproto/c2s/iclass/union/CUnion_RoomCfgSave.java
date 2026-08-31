package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取赛事成员审核列表
 *
 * @author zaf
 */
@Data
public class CUnion_RoomCfgSave extends CUnion_Base {
    /**
     * 勾选的游戏玩法Id列表
     */
    private List<Long> unionGameList = new ArrayList<>();


}