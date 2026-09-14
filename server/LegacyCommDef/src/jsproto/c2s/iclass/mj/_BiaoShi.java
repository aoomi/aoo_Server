package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.mj.template.MJTemplateWaitingExInfo;

import java.util.List;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class _BiaoShi extends SMJ_PiaoFen {

    public static _BiaoShi make(long roomID, int pos, List<MJTemplateWaitingExInfo> biaoShiList, String gameNameStr) {
        _BiaoShi ret = new _BiaoShi();
        ret.roomID = roomID;
        ret.biaoShiList = biaoShiList;
        ret.pos = pos;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}														
