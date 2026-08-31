package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 批量修改亲友圈疲劳值
 *
 * @author
 */
@Data
public class CClub_ModifyFatigueInterface extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 页数
     */
    private int pageNum;
    /**
     * 查询
     */
    private String query;

}