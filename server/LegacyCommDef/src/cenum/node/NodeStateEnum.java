package cenum.node;

/**
 * 服务节点状态
 */
public enum NodeStateEnum {
    /**
     * 正常节点状态
     */
    NORMAL,
    /**
     * 异常节点状态
     */
    ABNORMAL,;

    public final static boolean isNormal(int curNodeState){
        return NodeStateEnum.NORMAL.ordinal() == curNodeState;
    }

    public final static boolean isAbnormal(int curNodeState){
        return !isNormal(curNodeState);
    }
}
