package jsproto.c2s.cclass;

import lombok.Data;

@Data
public class MaintainGameInfo {
    //游戏ID
    private int gameTypeId;
    //开始维护时间
    private int startTime;
    //结束维护时间
    private int endTime;
    //标题
    private String title;
    //内容
    private String content;
    //主标题
    private String mainTitle;
    //0没有维护,1维护中
    private int status;
}
