package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 加入赛事
 *
 * @author zaf
 */
@Data
public class CUnion_Join extends BaseSendMsg {
    /**
     * 赛事编号
     */
    private int unionSign;

    /**
     * 亲友圈Id
     */
    private long clubId;

}