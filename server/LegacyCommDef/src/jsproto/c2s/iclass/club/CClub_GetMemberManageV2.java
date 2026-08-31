package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/** Cursor-based member management request for large clubs. */
public class CClub_GetMemberManageV2 extends BaseSendMsg {
    public long clubId;
    public long afterMemberId;
    public int limit = 50;
    /** 0 joined, 1 pending join, 2 pending exit. */
    public int pageType;
}
