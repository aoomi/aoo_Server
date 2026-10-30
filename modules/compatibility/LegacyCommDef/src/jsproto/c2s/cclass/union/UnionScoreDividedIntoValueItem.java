package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnionScoreDividedIntoValueItem {
    private double scoreDividedInto;
    private double scorePercent;

    public UnionScoreDividedIntoValueItem(double scoreDividedInto, double scorePercent) {
        this.scoreDividedInto = scoreDividedInto;
        this.scorePercent = scorePercent;
    }

    public static String getItemsName() {
        return "scoreDividedInto,scorePercent";
    }
}
