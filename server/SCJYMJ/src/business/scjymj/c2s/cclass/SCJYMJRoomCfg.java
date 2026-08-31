package business.scjymj.c2s.cclass;

import jsproto.c2s.cclass.room.BaseCreateRoom;

import java.util.ArrayList;
import java.util.List;

public class SCJYMJRoomCfg extends BaseCreateRoom {
    // 2房牌,3房牌
    public int paishu;
    // 自摸加番,自摸加底
    public int hutype;
    // 2番,3番，4番
    public int fengding;
    // 飘在内(选飘),飘在外(选飘),飘在内(座飘),飘在外(座飘),不飘
    public int piao;
    // 自动准备,庄闲玩法
    public List<Integer> other = new ArrayList<Integer>();
    // 2次,3次，4次
    public int jiesancishu;

    public int dianganghua;

    public int chajiao;
}

