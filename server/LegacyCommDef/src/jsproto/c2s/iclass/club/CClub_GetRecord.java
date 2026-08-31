package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
@Data
public class CClub_GetRecord extends BaseSendMsg {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 0今天,1昨天,2最近三天
     */
    private int getType;
    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 是否只显示已扣选的战绩 0:所有,1:隐藏已查看战绩
     */
    private int type;
    /**
     * 查询的房间id
     */
    private long query;

}