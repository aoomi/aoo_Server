package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.template.MJTemplate_XueLiuPlayerLiuSui;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class _XueLiu_LiuShui extends BaseSendMsg {
    /**
     *
     */
    public long roomID;
    public long pid;
    public int posId;
    public List<? extends MJTemplate_XueLiuPlayerLiuSui> liuShuiList = new ArrayList<>();

    public static _XueLiu_LiuShui make(long roomID, long pid, int posId, List<? extends MJTemplate_XueLiuPlayerLiuSui> liuShuiList, String gameNameStr) {
        _XueLiu_LiuShui liuShui = new _XueLiu_LiuShui();
        liuShui.roomID = roomID;
        liuShui.pid = pid;
        liuShui.posId = posId;
        liuShui.liuShuiList = liuShuiList;
        liuShui.setGameNameStr(gameNameStr);
        return liuShui;
    }


}		
