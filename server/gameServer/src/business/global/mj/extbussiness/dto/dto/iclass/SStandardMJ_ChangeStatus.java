package business.global.mj.extbussiness.dto.iclass;

import business.global.mj.extbussiness.dto.StandardMJWaitingExInfo;
import cenum.room.SetState;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;
import java.util.Objects;

/**
 * 接收客户端数据
 * 状态改变
 *
 * @author zaf
 */

public class SStandardMJ_ChangeStatus extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public int setID;//局数
    public SetState state;  //位置
    public String waitingExType;  //票分类型
    public List<StandardMJWaitingExInfo> biaoShiList;
    public int dPos = 0; // 庄家位置

    public static SStandardMJ_ChangeStatus make(long roomID, int setID, SetState state, int dPos, String waitingExType, List<StandardMJWaitingExInfo> biaoShiList, String gameNameStr) {
        SStandardMJ_ChangeStatus ret = new SStandardMJ_ChangeStatus();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.state = state;
        ret.dPos = dPos;
        ret.waitingExType = waitingExType;
        ret.biaoShiList = biaoShiList;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }

    public String getOpName() {
        if (Objects.isNull(getGameNameStr())) {
            return this.getClass().getSimpleName();
        } else {
            return String.format("S%s%s", getGameNameStr(), this.getClass().getSimpleName().replace( "SStandardMJ",""));
        }
    }
}										
