package business.global.mj.extbussiness.dto.iclass;

import business.global.mj.extbussiness.dto.StandardMJWaitingExInfo;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;
import java.util.Objects;

/**
 * 莆田麻将 接收客户端数据 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class SStandardMJ_BiaoShi extends BaseSendMsg {

    public long roomID;
    public List<StandardMJWaitingExInfo> biaoShiList;
    public int pos;

    public static SStandardMJ_BiaoShi make(long roomID, int pos, List<StandardMJWaitingExInfo> biaoShiList, String gameNameStr) {
        SStandardMJ_BiaoShi ret = new SStandardMJ_BiaoShi();
        ret.roomID = roomID;
        ret.biaoShiList = biaoShiList;
        ret.pos = pos;
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
