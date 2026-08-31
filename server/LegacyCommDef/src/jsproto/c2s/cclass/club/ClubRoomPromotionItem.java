package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.Objects;

/**
 * 房间推广员项
 *
 * @author
 */
@Data
public class ClubRoomPromotionItem {
    /**
     * 推广员id
     */
    private long partnerPid;
    /**
     * 亲友圈id
     */
    private long clubId;


    public ClubRoomPromotionItem(long partnerPid, long clubId) {
        this.partnerPid = partnerPid;
        this.clubId = clubId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ClubRoomPromotionItem that = (ClubRoomPromotionItem) o;
        return partnerPid == that.partnerPid &&
                clubId == that.clubId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), partnerPid, clubId);
    }
}
