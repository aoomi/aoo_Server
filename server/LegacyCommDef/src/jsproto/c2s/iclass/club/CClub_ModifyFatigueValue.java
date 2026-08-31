package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量修改亲友圈疲劳值
 *
 * @author
 */
@Data
public class CClub_ModifyFatigueValue extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 亲友圈成员Pid
     */
    private List<Long> pids = new ArrayList<>();
    /**
     * 值
     */
    private int value;
    /**
     * 0：默认，1：加，2：减
     */
    private int opType;

}