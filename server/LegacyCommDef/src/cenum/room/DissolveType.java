package cenum.room;

/**
 * 解散类型
 *
 * @author Administrator
 */
public enum DissolveType {
    ALL,//全部同意
    HALF,//一半同意
    ;

    public static DissolveType valueOf(int value) {
        for (DissolveType flow : DissolveType.values()) {
            if (flow.ordinal() == value) {
                return flow;
            }
        }
        return DissolveType.HALF;
    }
};