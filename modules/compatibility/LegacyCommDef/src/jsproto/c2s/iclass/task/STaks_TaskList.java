package jsproto.c2s.iclass.task;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.task.TaskItemInfo;

import java.util.List;

public class STaks_TaskList extends BaseSendMsg {
    public List<TaskItemInfo> taskItemInfos;
    public int drawMoney;

    public static STaks_TaskList make(List<TaskItemInfo> taskItemInfos, int drawMoney) {
        STaks_TaskList ret = new STaks_TaskList();
        ret.taskItemInfos = taskItemInfos;
        ret.drawMoney = drawMoney;
        return ret;
    }
}