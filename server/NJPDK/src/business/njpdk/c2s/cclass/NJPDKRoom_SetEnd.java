package business.njpdk.c2s.cclass;

import jsproto.c2s.cclass.room.RoomSetEndInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 资阳跑得快当局结束
 *
 * @author Clark
 */


// 一局结束的信息
public class NJPDKRoom_SetEnd extends RoomSetEndInfo {
    public int endTime = 0;
    public List<Integer> roomDoubleList = new ArrayList<>();            //房间倍数
    public List<NJPDKRoom_PosEnd> posResultList = new ArrayList<>(); // 每个玩家的结算
}

