package business.xcpdk.c2s.cclass;

import java.util.List;

/**
 * @author zhujianming
 * @date 2022-06-15 09:37
 */
public class XCPDK_Bomb {
    public int pos;
    public List<Integer> bombList;
    public boolean isLianDui;
    public boolean isLast;

    public XCPDK_Bomb(int pos, List<Integer> bombList, boolean isLianDui, boolean isLast) {
        this.pos = pos;
        this.bombList = bombList;
        this.isLianDui = isLianDui;
        this.isLast = isLast;
    }
}
