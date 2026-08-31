package jsproto.c2s.iclass.union;

/**
 * 赛事玩家分组
 *
 * @author zaf
 */
public class CUnion_GroupingPid extends CUnion_Base {
    private long groupingId;
    /**
     * 指定禁止Pid
     */
    private long pid;

    public long getGroupingId() {
        return groupingId;
    }

    public long getPid() {
        return pid;
    }
}