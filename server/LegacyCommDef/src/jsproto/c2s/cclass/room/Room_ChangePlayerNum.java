package jsproto.c2s.cclass.room;

import java.util.ArrayList;
import java.util.List;

public class Room_ChangePlayerNum {
    private int endSec = 0;
    private int createPos = 0;
    private List<Integer> posAgreeList = new ArrayList<>(); // 0未表态 1支持 2拒绝

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
