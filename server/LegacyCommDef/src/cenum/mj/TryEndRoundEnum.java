package cenum.mj;

/**
 * 尝试结束回合
 *
 * @author Administrator
 */
public enum TryEndRoundEnum {
    /**
     * 空操作
     */
    NOT,
    /**
     * 立即结束回合
     */
    ALL_AT_ONCE,
    /**
     * 等待所有操作完成
     */
    ALL_WAIT,
    ;
}