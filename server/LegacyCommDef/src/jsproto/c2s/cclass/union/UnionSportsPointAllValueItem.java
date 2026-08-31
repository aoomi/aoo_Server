package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnionSportsPointAllValueItem {
    private double sportsPointAll;

    public UnionSportsPointAllValueItem(double sportsPointAll) {
        this.sportsPointAll = sportsPointAll;
    }

    public static String getItemsName() {
        return "sportsPointAll";
    }
}
