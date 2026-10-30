package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
@Data
public class CClub_GetAllRoom extends BaseSendMsg {

    private long clubId;        //俱乐部ID 等于0获取所有的  大于0获取对应的俱乐部
    private int pageNum;
    /**
     * 查询的房间列表
     */
    private List<Long> roomKey = new ArrayList<>();

}