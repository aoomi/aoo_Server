package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnionSportsPointLogItem {

    private double scorePoint;

    public UnionSportsPointLogItem(double scorePoint) {
        this.scorePoint = scorePoint;
    }

    public static String getItemsName() {
        return "sum(scorePoint) as scorePoint";
    }
}
