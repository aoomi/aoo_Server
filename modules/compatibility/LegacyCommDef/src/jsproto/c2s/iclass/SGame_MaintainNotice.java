package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 游戏维护通知消息
 */
@Data
public class SGame_MaintainNotice extends BaseSendMsg {

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


    public static SGame_MaintainNotice make(int gameTypeId, int startTime, int endTime, String title, String content, String mainTitle, int status) {
        SGame_MaintainNotice ret = new SGame_MaintainNotice();
        ret.gameTypeId = gameTypeId;
        ret.startTime = startTime;
        ret.endTime = endTime;
        ret.title = title;
        ret.content = content;
        ret.mainTitle = mainTitle;
        ret.status = status;
        return ret;
    }


}