package jsproto.c2s.cclass;


import lombok.Data;

import java.io.Serializable;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
@Data
public abstract class BaseSendMsg implements Serializable {
    /**
     * 消息头部
     */
    public int Head;


    public String getClassName() {
        return this.getClass().getName();
    }

}
