package jsproto.c2s.cclass.room;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

/**
 * 竞技场房间配置
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class ArenaRoomConfig<T> implements Serializable {
    // 赛场ID
    private long aid = 0L;
    // 赛场名称
    private String arenaName;
    // 赛制状态 1.定局积分赛(默认)2.打立出局赛.3.瑞士移位赛
    private int formatType;
    // 赛制阶段配置
    private T cfgInfo;
    // 初始分数
    private int initPoint = 0;
    private int baseMark; // 基础分

    public long getAid() {
        return aid;
    }

    public void setAid(long aid) {
        this.aid = aid;
    }

    public String getArenaName() {
        return arenaName;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }

    public int getFormatType() {
        return formatType;
    }

    public void setFormatType(int formatType) {
        this.formatType = formatType;
    }

    public Object getCfgInfo() {
        return cfgInfo;
    }

    public void setCfgInfo(T cfgInfo) {
        this.cfgInfo = cfgInfo;
    }

    public int getInitPoint() {
        return initPoint;
    }

    public void setInitPoint(int initPoint) {
        this.initPoint = initPoint;
    }

    public ArenaRoomConfig(long aid, String arenaName, int formatType, T cfgInfo) {
        super();
        this.aid = aid;
        this.arenaName = arenaName;
        this.formatType = formatType;
        this.cfgInfo = cfgInfo;
    }

    public ArenaRoomConfig() {

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


    public int getBaseMark() {
        return this.baseMark;
    }


}
