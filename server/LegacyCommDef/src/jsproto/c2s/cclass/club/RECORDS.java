package jsproto.c2s.cclass.club;

import java.util.ArrayList;
import java.util.List;

public class RECORDS {
    List<RoomPromotionRevertInfo> RECORDS = new ArrayList<>();

    public List<RoomPromotionRevertInfo> getRECORDS() {
        return RECORDS;
    }

    public void setRECORDS(List<RoomPromotionRevertInfo> rECORDS) {
        RECORDS = rECORDS;
    }

    @Override
    public String toString() {
        return "RECORDS [RECORDS=" + RECORDS + "]";
    }


}
