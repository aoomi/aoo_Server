package jsproto.c2s.cclass.room;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间解散
 *
 * @author Administrator
 */
public class Room_Dissolve {
    // 结束时间
    private int endSec = 0;
    // 发起人位置Pos
    private int createPos = 0;
    // 0未表态 1支持 2拒绝
    private List<Integer> posAgreeList = new ArrayList<>();

    public int getEndSec() {
        return endSec;
    }

    public void setEndSec(int endSec) {
        this.endSec = endSec;
    }

    public int getCreatePos() {
        return createPos;
    }

    public void setCreatePos(int createPos) {
        this.createPos = createPos;
    }

    public List<Integer> getPosAgreeList() {
        return posAgreeList;
    }

    public void setPosAgreeList(List<Integer> posAgreeList) {
        this.posAgreeList = posAgreeList;
    }


}
