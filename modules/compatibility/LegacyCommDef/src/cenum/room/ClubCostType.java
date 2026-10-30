package cenum.room;

/**
 * 俱乐部消耗模式
 *
 * @author Administrator
 */
public enum ClubCostType {
    ROOM_CARD,// 房卡(金币):0
    CLUB_CARD,// 圈卡:1
    ;

    // 检查是否圈卡模式
    public static boolean isClubCard(int clubCostType) {
        return CLUB_CARD.ordinal() == clubCostType;
    }
}