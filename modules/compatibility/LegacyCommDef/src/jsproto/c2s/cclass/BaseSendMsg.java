package jsproto.c2s.cclass;

import cenum.VisitSignEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
@Data
public abstract class BaseSendMsg implements Serializable {
    private VisitSignEnum signEnum = null;
    private String gameNameStr = null;

    public String getOpName() {
        if (Objects.isNull(getGameNameStr())) {
            return this.getClass().getSimpleName();
        } else {
            return String.format("S%s%s", getGameNameStr(), this.getClass().getSimpleName());
        }
    }

}
