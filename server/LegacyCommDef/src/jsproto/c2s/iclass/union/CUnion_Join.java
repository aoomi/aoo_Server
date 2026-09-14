package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 加入联盟
 *
 * @author zaf
 */
@Data
public class CUnion_Join extends BaseSendMsg {
    /**
     * 联盟编号
     */
    private int unionSign;

    /**
     * 俱乐部Id
     */
    private long clubId;

}