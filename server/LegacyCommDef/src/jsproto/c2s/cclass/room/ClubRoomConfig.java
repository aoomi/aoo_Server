package jsproto.c2s.cclass.room;

import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

/**
 * 亲友圈房间配置
 *
 * @author Administrator
 */
@Data
@SuppressWarnings("serial")
public class ClubRoomConfig implements Serializable {

    // 房主ID
    private long ownerID;
    // 房间key
    private String roomKey = "";
    // 消耗房卡数
    private int roomCard = 0;
    // 亲友圈名称
    public String clubName;


    public ClubRoomConfig(long ownerID, String roomKey, int roomCard, String clubName) {
        super();
        this.ownerID = ownerID;
        this.roomKey = roomKey;
        this.roomCard = roomCard;
        this.clubName = clubName;
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
