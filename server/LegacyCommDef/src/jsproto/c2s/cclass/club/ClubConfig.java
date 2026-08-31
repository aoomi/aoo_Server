package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
@Data
public class ClubConfig extends BaseSendMsg {
    /**
     * 基础设置
     */
    private List<Integer> basics = new ArrayList<>();
    /**
     * 提出房间
     */
    private int kickOutRoom;
    /**
     * 解散设置
     */
    private int dissolveSet;
    /**
     * 解散时间
     */
    private int dissolveTime;


    public ClubConfig(List<Integer> basics, int kickOutRoom, int dissolveSet, int dissolveTime) {
        this.basics = basics;
        this.kickOutRoom = kickOutRoom;
        this.dissolveSet = dissolveSet;
        this.dissolveTime = dissolveTime;
    }
}
