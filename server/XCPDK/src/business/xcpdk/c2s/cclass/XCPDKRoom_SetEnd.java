package business.xcpdk.c2s.cclass;

import jsproto.c2s.cclass.room.RoomSetEndInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 跑得快 配置
 * @author Clark
 *
 */


// 一局结束的信息
public class XCPDKRoom_SetEnd extends RoomSetEndInfo{
	public int endTime = 0;
	public List<Integer> 	roomDoubleList = new ArrayList<>();			//房间倍数
	public List<XCPDKRoom_PosEnd> posResultList = new ArrayList<>(); // 每个玩家的结算
}

