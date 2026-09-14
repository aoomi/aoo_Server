package business.global.mj.extbussiness.dto.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.Objects;


@SuppressWarnings("serial")
public class SStandardMJ_SetEnd<T> extends BaseSendMsg {

    public long roomID;
    public T setEnd;
    public boolean roomEnd;

    public static <T> SStandardMJ_SetEnd<T> make(long roomID, T setEnd, boolean roomEnd, String gameNameStr) {
        SStandardMJ_SetEnd<T> ret = new SStandardMJ_SetEnd<T>();
        ret.roomID = roomID;
        ret.setEnd = setEnd;
        ret.roomEnd = roomEnd;
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
