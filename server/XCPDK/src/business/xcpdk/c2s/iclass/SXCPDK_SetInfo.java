package business.xcpdk.c2s.iclass;
import java.util.HashMap;
import java.util.Map;

public class SXCPDK_SetInfo {
    private long setID;
    // 每个人的分数
    private Map<Integer,Integer> point = new HashMap<>();
    public SXCPDK_SetInfo(long setID) {
        this.setID = setID;
    }

    public Map<Integer, Integer> getPoint() {
        return point;
    }

    public long getSetID() {
        return setID;
    }
}

