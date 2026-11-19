package business.global.room;

import cenum.PrizeType;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

/** Executable regression test for the union-room fail-closed path. */
public final class NormalRoomMgrCreationFailureSelfTest {
    public static void main(String[] args) {
        String previousShare = System.getProperty("isShare");
        try {
            System.setProperty("isShare", "0");
            BaseRoomConfigure<BaseCreateRoom> configure = new BaseRoomConfigure<>(
                    PrizeType.None, new GameType(999, "missing-test-game", 2), new BaseCreateRoom());
            if (NormalRoomMgr.getInstance().createUnionRoom(configure, "missing-test-room", 7L, 1, "test") != null) {
                throw new AssertionError("failed union-room creation must return null");
            }
            System.out.println("NormalRoomMgrCreationFailureSelfTest: PASS");
        } finally {
            if (previousShare == null) {
                System.clearProperty("isShare");
            } else {
                System.setProperty("isShare", previousShare);
            }
        }
    }
}
