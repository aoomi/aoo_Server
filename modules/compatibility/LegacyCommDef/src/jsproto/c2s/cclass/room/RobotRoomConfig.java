package jsproto.c2s.cclass.room;

import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

/**
 * 机器人房间配置
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
@Data
public class RobotRoomConfig implements Serializable {
    // 基础分
    private int baseMark;
    // 最小值
    private int min;
    // 最大值
    private int max;
    // 练习场ID
    private long practiceId = -1;

    public RobotRoomConfig() {
        super();
    }

    public RobotRoomConfig(int baseMark, int min, int max, long practiceId) {
        super();
        this.baseMark = baseMark;
        this.min = min;
        this.max = max;
        this.practiceId = practiceId;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
