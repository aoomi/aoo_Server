package jsproto.c2s.cclass.mj.template;

import jsproto.c2s.cclass.mj.MJRoomSetInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模版麻将当局信息
 *
 * @author Administrator
 */
@Data
public class MJTemplateRoomSetInfo extends MJRoomSetInfo {
    // 金
    private Integer jin1 = null;
    private Integer jin2 = null;
    private Integer jinJin = null;
    private Integer benJin;//翻开的那张 不是金
    public ArrayList<Integer> piaoFenList = null; // 飘分
    public String waitingExType;
    public List<MJTemplateWaitingExInfo> biaoShiList;

}
