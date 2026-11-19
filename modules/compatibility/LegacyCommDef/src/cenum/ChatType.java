package cenum;

/**
 * 聊天频道
 *
 * @author Administrator
 */
public enum ChatType {
    /**
     * 0-
     */
    NONE(0),
    /**
     * 1-世界频道
     */
    CHATTYPE_WORLD(1),
    /**
     * 2-公会频道
     */
    CHATTYPE_GUILD(2),
    /**
     * 3-好友频道
     */
    CHATTYPE_COMPANY(3),
    /**
     * 4-系统频道
     */
    CHATTYPE_SYSTEM(4),
    /**
     * 5-房间频道
     */
    CHATTYPE_ROOM(5),

    ;
    private int value;

    private ChatType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static ChatType valueOf(int value) {
        for (ChatType flow : ChatType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ChatType.NONE;
    }
}