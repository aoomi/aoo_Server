package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_SectionParam extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;

    /**
     * 目标pid
     */
    private long opPid;
    /**
     * 操作的亲友圈id
     */
    private long opClubId;
    /**
     * 区间信息的时候 是否赛事页面(1 是赛事 0 不是赛事)
     */
    private int unionFlag;
    /**
     * 联盟id
     */
    private long unionId;
    /**
     * 查看自己
     */
    private boolean isShowSelf;

}
