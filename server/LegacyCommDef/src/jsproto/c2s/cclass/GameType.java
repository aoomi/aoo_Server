package jsproto.c2s.cclass;

import cenum.ClassType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

/**
 * 游戏类型
 *
 * @author Clark
 */
public class GameType implements Serializable {
    // ID
    private int Id;
    // 名称
    private String Name;
    // 类型
    private int Type;

    public GameType(int id, String name, int type) {
        super();
        Id = id;
        Name = name;
        Type = type;
    }

    public GameType() {
        super();
    }

    public int getId() {
        return Id;
    }

    public String getName() {
        return Name.toUpperCase();
    }

    public ClassType getType() {
        return ClassType.valueOf(this.Type);
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
