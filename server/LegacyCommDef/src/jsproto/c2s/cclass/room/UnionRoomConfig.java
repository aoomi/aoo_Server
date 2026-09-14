package jsproto.c2s.cclass.room;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UnionRoomConfig implements Serializable {
    /**
     * 赛事名称
     */
    private String name;

    /**
     * 房主ID
     */
    private long ownerID;
    /**
     * 房间key
     */
    private String roomKey = "";
    /**
     * 消耗房卡数
     */
    private int roomCard = 0;

    public UnionRoomConfig(long ownerID, String roomKey, int roomCard, String name) {
        this.name = name;
        this.ownerID = ownerID;
        this.roomKey = roomKey;
        this.roomCard = roomCard;
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
