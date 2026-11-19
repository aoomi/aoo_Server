package jsproto.c2s.cclass.mj;

import cenum.room.RoomDissolutionState;
import jsproto.c2s.cclass.room.RoomSetEndInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局结束的信息
 *
 * @author Clark
 */

@Data
public class BaseMJRoom_SetEnd extends RoomSetEndInfo {
    private int endTime = 0;
    private int dPos;
    private int playBackCode;
    private RoomDissolutionState roomDissolutionState;
    private List<BaseMJRoom_PosEnd> posResultList = new ArrayList<>(); // 每个玩家的结算

    public void addPosHuList(BaseMJRoom_PosEnd posEnd) {
        this.posResultList.add(posEnd);
    }

}
