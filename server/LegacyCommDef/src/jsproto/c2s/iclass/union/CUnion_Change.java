package jsproto.c2s.iclass.union;

import lombok.Data;

@Data
public class CUnion_Change extends CUnion_Base {
    /**
     * pid
     */
    private long pid;
    /**
     * 亲友圈成员id
     */
    private long clubMemberId;

    public CUnion_Change(long unionId, long clubId, long pid, long clubMemberId) {
        super(unionId, clubId);
        this.pid = pid;
        this.clubMemberId = clubMemberId;
    }

    public CUnion_Change(long pid, long clubMemberId) {
        this.pid = pid;
        this.clubMemberId = clubMemberId;
    }
}
