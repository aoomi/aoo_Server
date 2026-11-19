package jsproto.c2s.iclass.union;

/**
 * 禁止游戏操作
 *
 * @author zaf
 */
public class CUnion_BanGame extends CUnion_Base {
    private long banPid;//指定禁止Pid
    private boolean isBan;//是否静止。

    public long getBanPid() {
        return banPid;
    }

    public boolean isBan() {
        return isBan;
    }
}