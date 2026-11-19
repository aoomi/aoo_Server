package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
public class SSCJYMJ_PiaoMai extends BaseSendMsg {

    public long roomID;
    public int piaoFen;  // -1 没有操作  0不飘分 1飘分
    public int shangHuo;  // -1 没有操作  0不上火 1上火
    public int pos;

    public static SSCJYMJ_PiaoMai make(long roomID, int pos, int piaoFen, int shangHuo) {
        SSCJYMJ_PiaoMai ret = new SSCJYMJ_PiaoMai();
        ret.roomID = roomID;
        ret.piaoFen = piaoFen;
        ret.shangHuo = shangHuo;
        ret.pos = pos;
        return ret;
    }
}
