package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 进入房间
 *
 * @author Administrator
 */
@Data
public class CBase_EnterRoom extends BaseSendMsg {
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 不用设置为位置 值为-1 否侧为设置固定位置
     */
    private int posID;
    /**
     * 传入亲友圈id
     */
    private long clubId;
    /**
     * 密码
     */
    private String password;

    /**
     * 是否存在快速加入房间
     */
    private boolean existQuickJoin;


}