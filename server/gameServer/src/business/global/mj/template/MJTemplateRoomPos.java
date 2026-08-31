package business.global.mj.template;

import business.global.room.base.AbsBaseRoom;
import business.global.room.mj.MJRoomPos;
import com.ddm.server.common.CommLogD;
import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Objects;

/**
 * 房间内每个位置信息
 *
 * @param <T>
 * @author Huaxing
 */
public class MJTemplateRoomPos<T> extends MJRoomPos {
    /**
     *
     */
    private MJTemplateWaitingExInfo waitingExInfo;

    public MJTemplateRoomPos(int posID, AbsBaseRoom room) {
        super(posID, room);
    }

    public void setWaitingExInfoFieldValue(MJTemplateRoomEnum.WaitingExType k, int value) {
        if (waitingExInfo == null) {
            CommLogD.error("waitingExInfo is not initialized");
            return;
        }
        try {
            Field declaredField = waitingExInfo.getClass().getDeclaredField(k.name().toLowerCase(Locale.ROOT));
            declaredField.setAccessible(true);
            declaredField.set(waitingExInfo, value);
            CommLogD.info("MJTemplateSetPos.waitingExInfo:" + waitingExInfo.toString());
        } catch (NoSuchFieldException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        } catch (IllegalAccessException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }

    public Integer getWaitingExInfoFieldValue(MJTemplateRoomEnum.WaitingExType k) {
        if (waitingExInfo == null) {
            return -1;
        }
        try {
            Field declaredField = waitingExInfo.getClass().getDeclaredField(k.name().toLowerCase(Locale.ROOT));
            declaredField.setAccessible(true);
            Integer integer = (Integer) declaredField.get(waitingExInfo);
            return Objects.nonNull(integer) ? integer : -1;
        } catch (NoSuchFieldException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        } catch (IllegalAccessException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return -1;
    }

    public MJTemplateWaitingExInfo getWaitingExInfo() {
        return waitingExInfo;
    }

    public void setWaitingExInfo(MJTemplateWaitingExInfo waitingExInfo) {
        this.waitingExInfo = waitingExInfo;
    }
}
