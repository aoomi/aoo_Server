package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@NoArgsConstructor
public class UnionScorePercentValueItem {
    private double scorePercent;


    public UnionScorePercentValueItem(double scorePercent) {
        this.scorePercent = scorePercent;
    }

    public static String getItemsName() {
        return "scorePercent";
    }
}
