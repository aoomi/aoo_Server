package business.global.pk.xcpdk;

import com.ddm.server.common.CommLogD;
import jsproto.c2s.cclass.pk.Victory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 跑得快一局游戏逻辑
 * @author zaf
 */

public  class XCPDKRoomSet_FJ extends XCPDKRoomSet{

	public ArrayList<Victory> roomDouble ;		//房间倍数

	@SuppressWarnings("rawtypes")
	public XCPDKRoomSet_FJ( XCPDKRoom room) {
		super(room);
		this.roomDouble 				= new ArrayList<Victory>();
	}

	/**
	 * @return m_RoomDouble
	 */
	@Override
	public int getRoomDouble(int pos) {
		return Math.max(1,  this.getNumByList(this.roomDouble, pos));
	}


	/**
	 */
	@Override
	public void addRoomDouble(int pos, int roomAddDouble) {
//		if (XCPDK_define.BombAlgorithm.PASS.has(this.room.getRoomCfg().zhadansuanfa)) {
//			return;
//		}
//		if(this.room.isWanFaByType(XCPDK_WANFA.XCPDK_WANFA_MAXZHADAN)  ){
//			if (this.m_AddRoomDoubleCount >= this.room.getConfigMgr().getMaxRoomAddDouble()) {
//				return;
//			} else {
//				this.m_AddRoomDoubleCount++;
//			}
//		}
//		this.addNumByList(this.roomDouble, pos, roomAddDouble);
	}

	/**
	 */
	@Override
	public void addNewRoomDouble(int pos, int bombNum,int bombScore) {
		Optional<Victory> first = this.roomDouble.stream().filter(z -> z.getPos() == pos).findFirst();
		if(first.isPresent()){
			Victory current = first.orElseThrow();
			int index = this.roomDouble.indexOf(current);
			this.roomDouble.set(index, new XCPDKVictory(pos, current.getNum() + bombNum,
					XCPDKVictory.bombScoreOf(current) + bombScore));
		}else{
			this.roomDouble.add(new XCPDKVictory(pos, bombNum, bombScore));
		}
	}

	@Override
	public void addNumByList(ArrayList<Victory> list, int pos, int num) {
		boolean flag = false;
		for (Victory victory : list) {
			if (victory == null) {
                return;
            }
			if (victory.getPos() == pos) {
				int count = 0;
				if(num != 0) {
					count = victory.getNum() != 0 ? victory.getNum() : 1;
				}
				num = num != 0 ? num : 1;
				victory.setNum(num + count);
				flag = true;
			}
		}
		if (!flag) {
			list.add(new Victory(pos, num));
		}
	}

	@Override
	protected List<Victory> getRoomDoubleList() {
		return roomDouble;
	}

	/*
	 * 获取list上对应的值
	 */
	@SuppressWarnings("finally")
	public int getNumByList(ArrayList<Victory> list, int pos) {
		int flag = -1;
		try {
			for (Victory vic : list) {
				if (null != vic && vic.getPos() == pos) {
					flag = vic.getNum();
					break;
				}
			}
			// CommLogD.info("getNumByList num:%s ,pos:%s", flag, pos);
			// if(this.openCardList.size() > 0) {
			// CommLogD.info("this.getNumByList.toString():%s",
			// list.toString());
			// }
		} catch (Exception e) {
			CommLogD.error("getNumByList error:" + e);
		}
		return flag;
	}
}
