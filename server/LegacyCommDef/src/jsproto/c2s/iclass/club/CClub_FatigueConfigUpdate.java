package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_FatigueConfigUpdate extends BaseSendMsg {
    /**
     * 亲友圈ID
     */
    private long clubId;
    /**
     * 疲劳值开关：T:打开，F:关闭
     */
    private boolean isOpen;
    /**
     * 疲劳值可见：0:管理与成员可见，1:仅管理可见
     */
    private int visible;
    /**
     * 疲劳值显示：0:亲友圈左上角显示当前疲劳值
     */
    private List<Integer> display = new ArrayList<Integer>();
    /**
     * 疲劳值清零：0:不清零，1:每日清零
     */
    private int empty;
    /**
     * 疲劳值
     */
    private int value;
}
