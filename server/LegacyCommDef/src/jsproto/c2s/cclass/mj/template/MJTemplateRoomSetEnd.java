package jsproto.c2s.cclass.mj.template;

import jsproto.c2s.cclass.mj.BaseMJRoom_SetEnd;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模版麻将房间结算
 *
 * @author Administrator
 */
@Data
public class MJTemplateRoomSetEnd extends BaseMJRoom_SetEnd {
    private Integer jin1;
    private Integer jin2;
    private Integer jinJin;
    private Integer benJin;//翻开的那张 不是金
    private List<Integer> maList = new ArrayList<>();
    private List<Integer> zhongList = new ArrayList<>();
    private Boolean zhongMa;//客户端要端 抓马动画

}
